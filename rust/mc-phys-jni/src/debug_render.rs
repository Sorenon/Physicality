use glow::{
    Context, HasContext, NativeBuffer, NativeProgram, NativeVertexArray, UniformLocation,
    VERTEX_SHADER,
};
use rapier3d::prelude::DebugRenderPipeline;

use crate::physics::{PhysicsWorld, RapierWorld};

static VERT_SHADER: &str = "
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aColor;

uniform mat4 mvp;

out vec3 color;

void main()
{
    gl_Position = mvp * vec4(aPos, 1.0);
    color = aColor;
}
";

static FRAG_SHADER: &str = "
#version 330 core
out vec4 FragColor;  
in vec3 color;
  
void main()
{
    FragColor = vec4(color, 1.0);
}
";

pub struct Renderer {
    pipeline: DebugRenderPipeline,
    renderer_backend: RenderBackend,
}

impl Renderer {
    pub fn new(context: Context) -> Self {
        Self {
            renderer_backend: unsafe { RenderBackend::new(context) },
            pipeline: DebugRenderPipeline::default(),
        }
    }

    pub fn render(&mut self, rapier_world: &RapierWorld, mvp: &[f32]) {
        unsafe {
            self.renderer_backend
                .context
                .bind_vertex_array(Some(self.renderer_backend.vao));
            self.renderer_backend
                .context
                .bind_buffer(glow::ARRAY_BUFFER, Some(self.renderer_backend.vbo));

            self.renderer_backend
                .context
                .use_program(Some(self.renderer_backend.program));
            self.renderer_backend.context.uniform_matrix_4_f32_slice(
                Some(&self.renderer_backend.uniform),
                false,
                mvp,
            );
        }

        self.pipeline.render(
            &mut self.renderer_backend,
            &rapier_world.rigid_body_set,
            &rapier_world.collider_set,
            &rapier_world.impulse_joint_set,
            &rapier_world.multibody_joint_set,
            &rapier_world.narrow_phase,
        );

        unsafe {
            self.renderer_backend.draw();
            self.renderer_backend.context.bind_vertex_array(None);
        }
    }
}

pub struct RenderBackend {
    context: Context,
    program: NativeProgram,
    vao: NativeVertexArray,
    vbo: NativeBuffer,
    uniform: UniformLocation,
    cpu_buffer: Vec<f32>,
}

impl RenderBackend {
    pub unsafe fn new(context: Context) -> Self {
        let vertex_shader = context.create_shader(glow::VERTEX_SHADER).unwrap();
        context.shader_source(vertex_shader, VERT_SHADER);
        context.compile_shader(vertex_shader);

        if !context.get_shader_compile_status(vertex_shader) {
            panic!(
                "vertex shader compilation failed {:?}",
                context.get_shader_info_log(vertex_shader)
            );
        }

        let fragment_shader = context.create_shader(glow::FRAGMENT_SHADER).unwrap();
        context.shader_source(fragment_shader, FRAG_SHADER);
        context.compile_shader(fragment_shader);

        if !context.get_shader_compile_status(fragment_shader) {
            panic!(
                "fragment shader compilation failed {:?}",
                context.get_shader_info_log(fragment_shader)
            );
        }

        let program = context.create_program().unwrap();
        context.attach_shader(program, vertex_shader);
        context.attach_shader(program, fragment_shader);
        context.link_program(program);

        if !context.get_program_link_status(program) {
            panic!(
                "program link failed {:?}",
                context.get_program_info_log(program)
            )
        }
        context.delete_shader(vertex_shader);
        context.delete_shader(fragment_shader);

        let vao = context.create_vertex_array().unwrap();
        let vbo = context.create_buffer().unwrap();

        let size_of_f32 = std::mem::size_of::<f32>() as i32;

        context.bind_buffer(glow::ARRAY_BUFFER, Some(vbo));
        context.buffer_data_size(glow::ARRAY_BUFFER, size_of_f32 * 6 * 128, glow::STREAM_DRAW);

        // position attribute
        context.vertex_attrib_pointer_f32(0, 3, glow::FLOAT, false, 6 * size_of_f32, 0);
        context.enable_vertex_attrib_array(0);
        // color attribute
        context.vertex_attrib_pointer_f32(
            1,
            3,
            glow::FLOAT,
            false,
            6 * size_of_f32,
            3 * size_of_f32,
        );
        context.enable_vertex_attrib_array(1);

        context.bind_vertex_array(None);

        Self {
            uniform: context.get_uniform_location(program, "mvp").unwrap(),
            program,
            vao,
            vbo,
            cpu_buffer: Vec::with_capacity(6 * 128),
            context,
        }
    }

    pub unsafe fn draw(&mut self) {
        if self.cpu_buffer.is_empty() {
            return;
        }

        let bytes: &[u8] = std::slice::from_raw_parts(
            self.cpu_buffer.as_ptr() as _,
            self.cpu_buffer.len() * std::mem::size_of::<f32>(),
        );

        self.context
            .buffer_sub_data_u8_slice(glow::ARRAY_BUFFER, 0, bytes);

        self.context
            .draw_arrays(glow::LINES, 0, self.cpu_buffer.len() as i32 / 6);
    }
}

impl rapier3d::pipeline::DebugRenderBackend for RenderBackend {
    fn draw_line(
        &mut self,
        _: rapier3d::prelude::DebugRenderObject,
        a: rapier3d::prelude::Point<rapier3d::prelude::Real>,
        b: rapier3d::prelude::Point<rapier3d::prelude::Real>,
        color: [f32; 4],
    ) {
        self.cpu_buffer.push(a.x);
        self.cpu_buffer.push(a.y);
        self.cpu_buffer.push(a.z);
        self.cpu_buffer.push(color[0]);
        self.cpu_buffer.push(color[1]);
        self.cpu_buffer.push(color[2]);
        self.cpu_buffer.push(b.x);
        self.cpu_buffer.push(b.y);
        self.cpu_buffer.push(b.z);
        self.cpu_buffer.push(color[0]);
        self.cpu_buffer.push(color[1]);
        self.cpu_buffer.push(color[2]);

        if self.cpu_buffer.len() == 6 * 128 {
            unsafe {
                self.draw();
            }
            self.cpu_buffer.clear();
        }
    }
}
