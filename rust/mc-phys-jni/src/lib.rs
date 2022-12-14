use glow::Context;
// This is the interface to the JVM that we'll call the majority of our
// methods on.
use jni::JNIEnv;

// These objects are what you should use as arguments to your native
// function. They carry extra lifetime information to prevent them escaping
// this context and getting used after being GC'd.
use jni::objects::{JClass, JObject, JValue};

// This is just a pointer. We'll be returning it from our function. We
// can't return one of the objects with lifetime information because the
// lifetime checker won't let us.
use jni::sys::{jfloat, jint, jlong};
use once_cell::sync::OnceCell;
use parking_lot::Mutex;
use rapier3d::na::{Matrix4, Quaternion, UnitQuaternion, Vector3, Vector4};
use rapier3d::prelude::{DebugRenderBackend, DebugRenderPipeline};
use std::cell::RefCell;
use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use thunderdome::{Arena, Index};

use physics::{make_block_colliders, Callback, CallbackContext, PhysicsWorld, FFI_AABB};

mod physics;

thread_local! {
    static PHYSICS_WORLDS: RefCell<Arena<PhysicsWorld>> = RefCell::new(Arena::new());
}

// static RENDERER: OnceCell<Mutex<Renderer>> = OnceCell::new();

// #[no_mangle]
// pub unsafe extern "system" fn Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_initializeDebugRenderer(
//     env: JNIEnv,
//     _class: JClass,
//     glfw_get_proc_address_ptr: jlong,
// ) -> jint {
//     RENDERER.get_or_init(|| {
//         type GlfwGetProcAddress = unsafe extern "C" fn(*const c_char) -> *const c_char;

//         let glfw_get_proc_address: GlfwGetProcAddress =
//             std::mem::transmute(glfw_get_proc_address_ptr);

//         Mutex::new(Renderer::new(Context::from_loader_function(|name| {
//             let cstr = CString::new(name).unwrap();
//             glfw_get_proc_address(cstr.as_ptr() as _) as _
//         })))
//     });

//     0
// }

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_debugRender(
    env: JNIEnv,
    _class: JClass,
    physics_world: jlong,
    callback: JObject,
) -> jint {
    PHYSICS_WORLDS.with(|worlds| {
        let mut worlds = worlds.borrow_mut();
        let index = Index::from_bits(physics_world as _).unwrap();
        let physics_world = worlds.get_mut(index).unwrap();
        let rapier = &physics_world.rapier;
        let mut backend = Backend { callback, env };

        DebugRenderPipeline::default().render(
            &mut backend,
            &rapier.rigid_body_set,
            &rapier.collider_set,
            &rapier.impulse_joint_set,
            &rapier.multibody_joint_set,
            &rapier.narrow_phase,
        )
    });

    0
}

struct Backend<'a> {
    env: JNIEnv<'a>,
    callback: JObject<'a>,
}

impl<'a> DebugRenderBackend for Backend<'a> {
    fn draw_line(
        &mut self,
        _: rapier3d::prelude::DebugRenderObject,
        a: rapier3d::prelude::Point<rapier3d::prelude::Real>,
        b: rapier3d::prelude::Point<rapier3d::prelude::Real>,
        color: [f32; 4],
    ) {
        self.env
            .call_method(self.callback, "renderLine", "(FFFFFFFFFF)V", &[
                JValue::Float(a.x),
                JValue::Float(a.y),
                JValue::Float(a.z),
                JValue::Float(b.x),
                JValue::Float(b.y),
                JValue::Float(b.z),
                JValue::Float(color[0]),
                JValue::Float(color[1]),
                JValue::Float(color[2]),
                JValue::Float(color[3]),
            ])
            .unwrap();
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_createPhysicsWorld(
    env: JNIEnv,
    _class: JClass,
    _level: JObject,
    callback: JObject,
) -> jlong {
    // let url = format!(
    //     "vscode://vadimcn.vscode-lldb/launch/config?{{'request':'attach','pid':{}}}",
    //     std::process::id()
    // );
    // std::process::Command::new(
    //     "C:\\Users\\soren\\AppData\\Local\\Programs\\Microsoft VS Code\\Code.exe",
    // )
    // .arg("--open-url")
    // .arg(url)
    // .output()
    // .unwrap();
    // std::thread::sleep(std::time::Duration::from_millis(2000)); // Wait for debugger to attach

    let out = PHYSICS_WORLDS.with(|worlds| {
        let mut worlds = worlds.borrow_mut();
        let physics_world = PhysicsWorld::new(Callback {
            object: env.new_global_ref(callback).unwrap(),
        });
        worlds.insert(physics_world)
    });
    out.to_bits() as i64
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_step(
    env: JNIEnv,
    _class: JClass,
    physics_world: jlong,
    delta_time: jfloat,
) -> jint {
    PHYSICS_WORLDS.with(|worlds| {
        let mut worlds = worlds.borrow_mut();
        let index = Index::from_bits(physics_world as _).unwrap();
        let physics_world = worlds.get_mut(index).unwrap();
        physics_world.step_physics_world(delta_time, env);
    });
    0
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_addPhysicsBody(
    _env: JNIEnv,
    _class: JClass,
    physics_world: jlong,
    x: jfloat,
    y: jfloat,
    z: jfloat,
    out: jlong,
) -> jint {
    let out = out as usize as *mut u64;

    PHYSICS_WORLDS.with(|worlds| {
        let mut worlds = worlds.borrow_mut();
        let index = Index::from_bits(physics_world as _).unwrap();
        let physics_world = worlds.get_mut(index).unwrap();
        match physics_world.add_physics_body(x, y, z) {
            Ok(res) => {
                *out = std::mem::transmute(res);
                0
            }
            Err(err) => err,
        }
    })
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_getBodyPosition(
    env: JNIEnv,
    _class: JClass,
    physics_world: jlong,
    body: jlong,
    position_out: JObject,
) -> jint {
    PHYSICS_WORLDS.with(|worlds| {
        let mut worlds = worlds.borrow_mut();
        let index = Index::from_bits(physics_world as _).unwrap();
        let physics_world = worlds.get_mut(index).unwrap();
        match physics_world.get_body_translation(std::mem::transmute(body)) {
            Ok(pos) => {
                env.set_field(position_out, "x", "F", jni::objects::JValue::Float(pos.x))
                    .unwrap();
                env.set_field(position_out, "y", "F", jni::objects::JValue::Float(pos.y))
                    .unwrap();
                env.set_field(position_out, "z", "F", jni::objects::JValue::Float(pos.z))
                    .unwrap();

                0
            }
            Err(err) => err,
        }
    })
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_getBodyRenderTransform(
    env: JNIEnv,
    _class: JClass,
    physics_world: jlong,
    body: jlong,
    position_out: JObject,
    orientation_out: JObject,
) -> jint {
    PHYSICS_WORLDS.with(|worlds| {
        let mut worlds = worlds.borrow_mut();
        let index = Index::from_bits(physics_world as _).unwrap();
        let physics_world = worlds.get_mut(index).unwrap();
        match physics_world.get_render_transform(std::mem::transmute(body)) {
            Ok((pos, orientation)) => {
                env.set_field(position_out, "x", "F", jni::objects::JValue::Float(pos.x))
                    .unwrap();
                env.set_field(position_out, "y", "F", jni::objects::JValue::Float(pos.y))
                    .unwrap();
                env.set_field(position_out, "z", "F", jni::objects::JValue::Float(pos.z))
                    .unwrap();

                env.set_field(
                    orientation_out,
                    "x",
                    "F",
                    jni::objects::JValue::Float(orientation.i),
                )
                .unwrap();
                env.set_field(
                    orientation_out,
                    "y",
                    "F",
                    jni::objects::JValue::Float(orientation.j),
                )
                .unwrap();
                env.set_field(
                    orientation_out,
                    "z",
                    "F",
                    jni::objects::JValue::Float(orientation.k),
                )
                .unwrap();
                env.set_field(
                    orientation_out,
                    "w",
                    "F",
                    jni::objects::JValue::Float(orientation.w),
                )
                .unwrap();

                0
            }
            Err(err) => err,
        }
    })
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_blockUpdated(
    _env: JNIEnv,
    _class: JClass,
    physics_world: jlong,
    x: jint,
    y: jint,
    z: jint,
    addr: jlong,
    len: jlong,
) -> jint {
    PHYSICS_WORLDS.with(|worlds| {
        let mut worlds = worlds.borrow_mut();
        let index = Index::from_bits(physics_world as _).unwrap();
        let physics_world = worlds.get_mut(index).unwrap();

        let pos = Vector3::new(x, y, z);

        if let Some(old_colliders) = physics_world.blocks.get(&pos).unwrap() {
            for handle in old_colliders {
                physics_world.rapier.remove_collider(*handle);
            }
        }

        let aabbs = std::slice::from_raw_parts(addr as usize as *const FFI_AABB, len as usize);

        if aabbs.is_empty() {
            physics_world.blocks.insert(pos, None);
        } else {
            physics_world.blocks.insert(
                pos,
                Some(make_block_colliders(
                    pos,
                    aabbs,
                    &mut physics_world.rapier.collider_set,
                )),
            );
        }
    });

    0
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_sendBlockInfo(
    _env: JNIEnv,
    _class: JClass,
    callback_context: jlong,
    index: jint,
    addr: jlong,
    len: jlong,
) -> jint {
    let callback_context = &mut *(callback_context as *mut CallbackContext);
    callback_context.revive_block_info(
        index,
        std::slice::from_raw_parts(addr as usize as *const FFI_AABB, len as usize),
    );
    0
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_addCuboid(
    _env: JNIEnv,
    _class: JClass,
    physics_world: jlong,
    x: jfloat,
    y: jfloat,
    z: jfloat,
    ox: jfloat,
    oy: jfloat,
    oz: jfloat,
    ow: jfloat,
    shapes: jlong,
    shapes_len: jint,
    out: jlong,
) -> jint {
    let out = out as usize as *mut u64;

    let shapes = std::slice::from_raw_parts(
        shapes as *const (Vector3<f32>, Vector3<f32>),
        shapes_len as usize,
    );

    PHYSICS_WORLDS.with(|worlds| {
        let mut worlds = worlds.borrow_mut();
        let index = Index::from_bits(physics_world as _).unwrap();
        let physics_world = worlds.get_mut(index).unwrap();

        let pos = Vector3::new(x, y, z);
        let orientation =
            UnitQuaternion::new_normalize(Quaternion::from_vector(Vector4::new(ox, oy, oz, ow)));

        match physics_world.add_cuboids(pos, orientation, &shapes) {
            Ok(res) => {
                *out = std::mem::transmute(res);
                0
            }
            Err(err) => err,
        }
    })
}
