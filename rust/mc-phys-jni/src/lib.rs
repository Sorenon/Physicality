use std::collections::HashMap;

// This is the interface to the JVM that we'll call the majority of our
// methods on.
use jni::JNIEnv;

// These objects are what you should use as arguments to your native
// function. They carry extra lifetime information to prevent them escaping
// this context and getting used after being GC'd.
use jni::objects::{GlobalRef, JClass, JObject, JString, JValue};

// This is just a pointer. We'll be returning it from our function. We
// can't return one of the objects with lifetime information because the
// lifetime checker won't let us.
use jni::sys::{jfloat, jint, jlong, jobject, jstring};
use rapier3d::na::Vector3;

use physics::{PhysicsWorld, CallbackContext, FFI_AABB, Callback, PHYSICS_WORLDS, make_colliders};

mod physics;

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physv2_PhysJNI_createPhysicsWorld(
    env: JNIEnv,
    class: JClass,
    level: JObject,
    callback: JObject,
) -> jint {
    let url = format!(
        "vscode://vadimcn.vscode-lldb/launch/config?{{'request':'attach','pid':{}}}",
        std::process::id()
    );
    std::process::Command::new(
        "C:\\Users\\soren\\AppData\\Local\\Programs\\Microsoft VS Code\\Code.exe",
    )
    .arg("--open-url")
    .arg(url)
    .output()
    .unwrap();
    std::thread::sleep(std::time::Duration::from_millis(2000)); // Wait for debugger to attach

    return physics::create_physics_world(Callback {
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
    return physics::step_physics_world(physics_world as usize - 1, delta_time, env) as i32;
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

    match physics::add_physics_body(physics_world as usize - 1, x, y, z) {
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
    match physics::get_body_translation(physics_world as usize - 1, std::mem::transmute(body)) {
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
    match physics::get_render_transform(physics_world as usize - 1, std::mem::transmute(body)) {
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
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physv2_PhysJNI_blockUpdated(
    env: JNIEnv,
    class: JClass,
    physics_world: jint,
    x: jint,
    y: jint,
    z: jint,
    addr: jlong,
    len: jlong,
)  -> jint {
    let mut lock = PHYSICS_WORLDS.lock();
    let physics_world = lock.get_mut(physics_world as usize - 1).unwrap(); 

    let pos = Vector3::new(x, y, z);

    if let Some(old_colliders) = physics_world.blocks.get(&pos).unwrap() {
        for handle in old_colliders {
            physics_world.collider_set.remove(*handle, &mut physics_world.island_manager, &mut physics_world.rigid_body_set, false);
        }
    }
    
    let aabbs = std::slice::from_raw_parts(addr as usize as *const FFI_AABB, len as usize);

    if aabbs.len() == 0 {
        physics_world.blocks.insert(pos, None);
    } else {
        physics_world.blocks.insert(pos, Some(make_colliders(pos, aabbs, &mut physics_world.collider_set)));
    }

    return 0;
}

#[no_mangle]
pub unsafe extern "system" fn Java_net_sorenon_physicality_physv2_PhysJNI_sendBlockInfo(
    env: JNIEnv,
    class: JClass,
    callback_context: jlong,
    index: jint,
    addr: jlong,
    len: jlong,
) -> jint {
    let callback_context = &mut *(callback_context as *mut CallbackContext);
    callback_context.revive_block_info(index, std::slice::from_raw_parts(addr as usize as *const FFI_AABB, len as usize));
    return 0;
}
