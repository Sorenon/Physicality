use std::{
    cell::{Ref, RefCell},
    collections::{HashMap, HashSet},
};

use jni::{
    objects::{GlobalRef, JValue},
    sys::{jint, jlong},
    JNIEnv,
};
use once_cell::sync::Lazy;
use parking_lot::{Mutex, MutexGuard, ReentrantMutex};
use rapier3d::{
    na::{Point3, UnitQuaternion, Vector3},
    parry::bounding_volume::BoundingVolume,
    prelude::*,
};

pub struct PhysicsWorld {
    pub rigid_body_set: RigidBodySet,
    pub collider_set: ColliderSet,
    pub gravity: Vector3<f32>,
    pub integration_parameters: IntegrationParameters,
    pub physics_pipeline: PhysicsPipeline,
    pub island_manager: IslandManager,
    pub broad_phase: BroadPhase,
    pub narrow_phase: NarrowPhase,
    pub impulse_joint_set: ImpulseJointSet,
    pub multibody_joint_set: MultibodyJointSet,
    pub ccd_solver: CCDSolver,

    pub callback: Callback,
    pub delta_time: f32,
    pub blocks: HashMap<Vector3<i32>, Option<Vec<ColliderHandle>>>,
}

#[repr(C)]
pub struct FFI_AABB {
    min_x: f32,
    min_y: f32,
    min_z: f32,
    max_x: f32,
    max_y: f32,
    max_z: f32,
}

pub struct CallbackContext<'a> {
    last_index: i32,
    positions: &'a Vec<Vector3<i32>>,
    blocks: &'a mut HashMap<Vector3<i32>, Option<Vec<ColliderHandle>>>,
    collider_set: &'a mut ColliderSet,
}

#[test]
fn test() {
    let mut collider = ColliderBuilder::cuboid(0.5, 0.5, 0.5).build();
    collider.set_position(Isometry::new(
        Vector3::zeros(),
        Vector3::new(1., 1., 0.).normalize() * 45f32.to_radians(),
    ));
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
    let physics_world = PhysicsWorld {
        rigid_body_set: RigidBodySet::new(),
        collider_set: ColliderSet::new(),
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

        let mut wanted_blocks = HashSet::new();

        for (_, body) in physics_world.rigid_body_set.iter() {
            if body.is_dynamic() && !body.is_sleeping() {
                let mut aabb: Option<AABB> = None;

                for collider in body
                    .colliders()
                    .iter()
                    .map(|handle| physics_world.collider_set.get(*handle).unwrap())
                {
                    let shape_aabb = collider.compute_aabb();

                    match aabb {
                        Some(mut aabb) => aabb.merge(&shape_aabb),
                        None => aabb = Some(shape_aabb),
                    }
                }

                if let Some(aabb) = aabb {
                    let min_x = (aabb.mins.x - 0.01).floor() as i32 - 1;
                    let min_y = (aabb.mins.y - 0.01).floor() as i32 - 1;
                    let min_z = (aabb.mins.z - 0.01).floor() as i32 - 1;

                    let max_x = (aabb.maxs.x + 0.01).ceil() as i32 + 1;
                    let max_y = (aabb.maxs.y + 0.01).ceil() as i32 + 1;
                    let max_z = (aabb.maxs.z + 0.01).ceil() as i32 + 1;

                    for x in min_x..=max_x {
                        for y in min_y..=max_y {
                            for z in min_z..=max_z {
                                let pos = Vector3::new(x, y, z);
                                if !physics_world.blocks.contains_key(&pos)
                                    && !wanted_blocks.contains(&pos)
                                {
                                    wanted_blocks.insert(pos);
                                }
                            }
                        }
                    }
                }
            }
        }

        physics_world.callback.run_callback(
            env,
            wanted_blocks.into_iter().collect::<Vec<_>>(),
            &mut physics_world.blocks,
            &mut physics_world.collider_set,
        );

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

pub struct Callback {
    pub object: GlobalRef,
}

impl Callback {
    pub fn run_callback(
        &self,
        env: JNIEnv,
        wanted_blocks: Vec<Vector3<i32>>,
        blocks: &mut HashMap<Vector3<i32>, Option<Vec<ColliderHandle>>>,
        collider_set: &mut ColliderSet,
    ) {
        let mut callback_context = CallbackContext {
            last_index: -1,
            positions: &wanted_blocks,
            blocks,
            collider_set,
        };

        env.call_method(
            &self.object,
            "preStep",
            "(JJI)V",
            &[
                JValue::Long(std::ptr::addr_of_mut!(callback_context) as jlong),
                JValue::Long(wanted_blocks.as_ptr() as jlong),
                JValue::Int(wanted_blocks.len() as jint),
            ],
        )
        .unwrap();

        for i in (callback_context.last_index + 1)..wanted_blocks.len() as i32 {
            callback_context
                .blocks
                .insert(callback_context.positions[i as usize], None);
        }

        for pos in wanted_blocks {
            assert!(blocks.contains_key(&pos));
        }
    }
}

impl<'a> CallbackContext<'a> {
    pub fn revive_block_info(&mut self, index: i32, aabbs: &[FFI_AABB]) {
        for i in (self.last_index + 1)..index {
            assert!(self.blocks.insert(self.positions[i as usize], None).is_none());
        }

        self.last_index = index;

        let pos = self.positions[index as usize];

        assert!(self.blocks
            .insert(pos, Some(make_colliders(pos, aabbs, self.collider_set))).is_none());
    }
}

pub fn make_colliders(
    pos: Vector3<i32>,
    aabbs: &[FFI_AABB],
    collider_set: &mut ColliderSet,
) -> Vec<ColliderHandle> {
    let mut colliders = Vec::with_capacity(aabbs.len());

    for aabb in aabbs {
        let hx = (aabb.max_x - aabb.min_x) / 2.;
        let hy = (aabb.max_y - aabb.min_y) / 2.;
        let hz = (aabb.max_z - aabb.min_z) / 2.;

        colliders.push(
            collider_set.insert(
                ColliderBuilder::cuboid(hx, hy, hz)
                    .translation(Vector3::new(
                        pos.x as f32 + aabb.min_x + hx,
                        pos.y as f32 + aabb.min_y + hy,
                        pos.z as f32 + aabb.min_z + hz,
                    ))
                    .build(),
            ),
        );
    }

    colliders
}
