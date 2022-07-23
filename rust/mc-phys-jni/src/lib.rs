// This is the interface to the JVM that we'll call the majority of our
// methods on.
use jni::JNIEnv;

// These objects are what you should use as arguments to your native
// function. They carry extra lifetime information to prevent them escaping
// this context and getting used after being GC'd.
use jni::objects::{JClass, JObject, JString, JValue, GlobalRef};

// This is just a pointer. We'll be returning it from our function. We
// can't return one of the objects with lifetime information because the
// lifetime checker won't let us.
use jni::sys::{jfloat, jint, jlong, jobject, jstring};
use rapier3d::na::Vector3;
use rapier3d::prelude::EventHandler;

mod safe;

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physv2_PhysJNI_createPhysicsWorld(
    env: JNIEnv,
    class: JClass,
    level: JObject,
    callback: JObject,
) -> jint {
    return safe::create_physics_world(Callback {
        object: env.new_global_ref(callback).unwrap(),
    }) as i32;
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physv2_PhysJNI_step(
    env: JNIEnv,
    class: JClass,
    physics_world: jint,
    delta_time: jfloat,
) -> jint {
    return safe::step_physics_world(physics_world as usize - 1, delta_time, env) as i32;
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physv2_PhysJNI_addPhysicsBody(
    env: JNIEnv,
    class: JClass,
    physics_world: jint,
    x: jfloat,
    y: jfloat,
    z: jfloat,
    out: jlong,
) -> jint {
    let out = out as usize as *mut u64;

    match safe::add_physics_body(physics_world as usize - 1, x, y, z) {
        Ok(res) => {
            *out = std::mem::transmute(res);
            0
        }
        Err(err) => err,
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physv2_PhysJNI_getBodyPosition(
    env: JNIEnv,
    class: JClass,
    physics_world: jint,
    body: jlong,
    position_out: JObject,
) -> jint {
    match safe::get_body_translation(physics_world as usize - 1, std::mem::transmute(body)) {
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
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physv2_PhysJNI_getBodyRenderTransform(
    env: JNIEnv,
    class: JClass,
    physics_world: jint,
    body: jlong,
    position_out: JObject,
    orientation_out: JObject,
) -> jint {
    match safe::get_render_transform(physics_world as usize - 1, std::mem::transmute(body)) {
        Ok((pos, orientation)) => {
            // env.set_field(position_out, "x", "F", jni::objects::JValue::Float(pos.x))
            //     .unwrap();
            // env.set_field(position_out, "y", "F", jni::objects::JValue::Float(pos.y))
            //     .unwrap();
            // env.set_field(position_out, "z", "F", jni::objects::JValue::Float(pos.z))
            //     .unwrap();

            // env.set_field(
            //     orientation_out,
            //     "x",
            //     "F",
            //     jni::objects::JValue::Float(orientation.i),
            // )
            // .unwrap();
            // env.set_field(
            //     orientation_out,
            //     "y",
            //     "F",
            //     jni::objects::JValue::Float(orientation.j),
            // )
            // .unwrap();
            // env.set_field(
            //     orientation_out,
            //     "z",
            //     "F",
            //     jni::objects::JValue::Float(orientation.k),
            // )
            // .unwrap();
            // env.set_field(
            //     orientation_out,
            //     "w",
            //     "F",
            //     jni::objects::JValue::Float(orientation.w),
            // )
            // .unwrap();

            0
        }
        Err(err) => err,
    }
}

pub struct Callback {
    object: GlobalRef,
}

impl Callback {
    pub fn run_callback(&self, env: JNIEnv, wanted_blocks: &[Vector3<i32>]) {
        //This is safe??
        // let _res = env
        //     .call_method(
        //         &self.object,
        //         "preStep",
        //         "(JIJJ)V",
        //         &[
        //             JValue::Long(wanted_blocks.as_ptr() as jlong),
        //             JValue::Int(wanted_blocks.len() as jint),
        //             JValue::Long(0i64),
        //             JValue::Long(0i64),
        //         ],
        //     )
        //     .unwrap();
    }
}
