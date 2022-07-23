use std::collections::{HashMap, HashSet};

use jni::JNIEnv;
use once_cell::sync::Lazy;
use parking_lot::Mutex;
use rapier3d::{
    na::{UnitQuaternion, Vector3, Point3},
    parry::bounding_volume::BoundingVolume,
    prelude::*,
};

use crate::Callback;

pub struct PhysicsWorld {
    rigid_body_set: RigidBodySet,
    collider_set: ColliderSet,
    gravity: Vector3<f32>,
    integration_parameters: IntegrationParameters,
    physics_pipeline: PhysicsPipeline,
    island_manager: IslandManager,
    broad_phase: BroadPhase,
    narrow_phase: NarrowPhase,
    impulse_joint_set: ImpulseJointSet,
    multibody_joint_set: MultibodyJointSet,
    ccd_solver: CCDSolver,

    callback: Callback,
    delta_time: f32,
    blocks: HashMap<Vector3<i32>, RigidBodyHandle>,
    cache_set: HashSet<Vector3<i32>>
}

#[test]
fn test() {
    let mut collider = ColliderBuilder::cuboid(0.5, 0.5, 0.5).build();
    collider.set_position(Isometry::new(Vector3::zeros(), Vector3::new(1., 1., 0.).normalize() * 45f32.to_radians()));
    let aabb = collider.compute_aabb();

    let aabb = AABB {
        mins: Point3::new(-5.0, 2.0, -1.0),
        maxs: Point3::new(-4.0, 3.0, 0.0),
    };

    let min_x = (aabb.mins.x - 0.01).floor() as i32;
    let min_y = (aabb.mins.y - 0.01).floor() as i32;
    let min_z = (aabb.mins.z - 0.01).floor() as i32;

    let max_x = (aabb.maxs.x + 0.01).ceil() as i32;
    let max_y = (aabb.maxs.y + 0.01).ceil() as i32;
    let max_z = (aabb.maxs.z + 0.01).ceil() as i32;

    let mut wanted = Vec::new();

    for x in min_x..=max_x {
        for y in min_y..=max_y {
            for z in min_z..=max_z {
                let pos = Vector3::new(x, y, z);
                wanted.push(pos);
            }
        }
    }

    // let phys = unsafe {
    //     create_physics_world(std::mem::zeroed())
    // };

    // collider = 
}

pub static PHYSICS_WORLDS: Lazy<Mutex<Vec<PhysicsWorld>>> = Lazy::new(|| Mutex::new(Vec::new()));

pub fn create_physics_world(callback: Callback) -> usize {
    let mut collider_set = ColliderSet::new();

    /* Create the ground. */
    let collider = ColliderBuilder::cuboid(100.0, 0.1, 100.0).build();
    collider_set.insert(collider);

    let physics_world = PhysicsWorld {
        rigid_body_set: RigidBodySet::new(),
        collider_set,
        gravity: vector![0.0, -9.81, 0.0],
        integration_parameters: IntegrationParameters::default(),
        physics_pipeline: PhysicsPipeline::new(),
        island_manager: IslandManager::new(),
        broad_phase: BroadPhase::new(),
        narrow_phase: NarrowPhase::new(),
        impulse_joint_set: ImpulseJointSet::new(),
        multibody_joint_set: MultibodyJointSet::new(),
        ccd_solver: CCDSolver::new(),

        delta_time: 0.,
        callback,
        blocks: HashMap::new(),
        cache_set: HashSet::new(),
    };

    let mut physics_worlds = PHYSICS_WORLDS.lock();
    physics_worlds.push(physics_world);
    return physics_worlds.len();
}

pub fn step_physics_world(index: usize, delta_time: f32, env: JNIEnv) -> i32 {
    let mut lock = PHYSICS_WORLDS.lock();
    let mut physics_world = match lock.get_mut(index) {
        Some(physics_world) => physics_world,
        None => return -1,
    };

    physics_world.delta_time += delta_time;

    while physics_world.delta_time > 1. / 60. {
        physics_world.delta_time -= 1. / 60.;

        // let mut wanted_blocks = HashSet::new();
        let wanted_blocks = &mut physics_world.cache_set;

        // for (_, body) in physics_world.rigid_body_set.iter() {
        //     if body.is_dynamic() && !body.is_sleeping() {
        //         let mut aabb: Option<AABB> = None;

        //         for collider in body
        //             .colliders()
        //             .iter()
        //             .map(|handle| physics_world.collider_set.get(*handle).unwrap())
        //         {
        //             let shape_aabb = collider.compute_aabb();

        //             match aabb {
        //                 Some(mut aabb) => aabb.merge(&shape_aabb),
        //                 None => aabb = Some(shape_aabb),
        //             }
        //         }

        //         if let Some(aabb) = aabb {
        //             let min_x = (aabb.mins.x - 0.01).floor() as i32;
        //             let min_y = (aabb.mins.y - 0.01).floor() as i32;
        //             let min_z = (aabb.mins.z - 0.01).floor() as i32;

        //             let max_x = (aabb.maxs.x + 0.01).ceil() as i32;
        //             let max_y = (aabb.maxs.y + 0.01).ceil() as i32;
        //             let max_z = (aabb.maxs.z + 0.01).ceil() as i32;

        //             // for x in min_x..=max_x {
        //             //     for y in min_y..=max_y {
        //             //         for z in min_z..=max_z {
        //             //             let pos = Vector3::new(x, y, z);
        //             //             if !physics_world.blocks.contains_key(&pos) && !wanted_blocks.contains(&pos) {
        //             //                 wanted_blocks.insert(pos);
        //             //             }
        //             //         }
        //             //     }
        //             // }
        //         }
        //     }
        // }

        let wanted_blocks = wanted_blocks.iter().copied().collect::<Vec<_>>();

        physics_world.callback.run_callback(env, &wanted_blocks);

        for pos in wanted_blocks {
            physics_world.blocks.insert(pos, RigidBodyHandle::from_raw_parts(0, 0));
        }

        physics_world.physics_pipeline.step(
            &mut physics_world.gravity,
            &physics_world.integration_parameters,
            &mut physics_world.island_manager,
            &mut physics_world.broad_phase,
            &mut physics_world.narrow_phase,
            &mut physics_world.rigid_body_set,
            &mut physics_world.collider_set,
            &mut physics_world.impulse_joint_set,
            &mut physics_world.multibody_joint_set,
            &mut physics_world.ccd_solver,
            &(),
            &(),
        );
    }

    return 0;
}

pub fn add_physics_body(index: usize, x: f32, y: f32, z: f32) -> Result<(u32, u32), i32> {
    let mut lock = PHYSICS_WORLDS.lock();
    let physics_world = match lock.get_mut(index) {
        Some(physics_world) => physics_world,
        None => return Err(-1),
    };

    let shape = ColliderBuilder::cuboid(0.5, 0.5, 0.5).build();
    let body = RigidBodyBuilder::dynamic()
        .translation(Vector3::new(x, y, z))
        .build();

    let body = physics_world.rigid_body_set.insert(body);
    physics_world
        .collider_set
        .insert_with_parent(shape, body, &mut physics_world.rigid_body_set);

    return Ok(body.into_raw_parts());
}

pub fn get_body_translation(index: usize, body: (u32, u32)) -> Result<Vector3<f32>, i32> {
    let lock = PHYSICS_WORLDS.lock();
    let physics_world = match lock.get(index) {
        Some(physics_world) => physics_world,
        None => return Err(-1),
    };

    match physics_world
        .rigid_body_set
        .get(RigidBodyHandle::from_raw_parts(body.0, body.1))
    {
        Some(body) => Ok(*body.translation()),
        None => Err(-1),
    }
}

pub fn get_render_transform(
    index: usize,
    body: (u32, u32),
) -> Result<(Vector3<f32>, UnitQuaternion<f32>), i32> {
    let lock = PHYSICS_WORLDS.lock();
    let physics_world = match lock.get(index) {
        Some(physics_world) => physics_world,
        None => return Err(-1),
    };

    match physics_world
        .rigid_body_set
        .get(RigidBodyHandle::from_raw_parts(body.0, body.1))
    {
        Some(body) => Ok((*body.translation(), *body.rotation())),
        None => Err(-1),
    }
}
