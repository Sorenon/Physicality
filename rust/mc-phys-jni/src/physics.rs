use std::collections::{HashMap, HashSet};

use jni::{
    objects::{GlobalRef, JValue},
    sys::{jint, jlong},
    JNIEnv,
};
use once_cell::sync::Lazy;
use parking_lot::Mutex;
use rapier3d::{
    na::{Isometry3, UnitQuaternion, Vector3},
    parry::bounding_volume::BoundingVolume,
    prelude::*,
};

pub static STEP_TIME: f32 = 1. / 60.;

pub struct PhysicsWorld {
    pub rapier: RapierWorld,
    pub callback: Callback,
    pub delta_time: f32,
    pub blocks: HashMap<Vector3<i32>, Option<Vec<ColliderHandle>>>,
    pub old_transforms: Vec<Isometry3<f32>>,
}

pub struct RapierWorld {
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
}

impl RapierWorld {
    fn new() -> Self {
        Self {
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
        }
    }

    fn step(&mut self) {
        self.physics_pipeline.step(
            &mut self.gravity,
            &self.integration_parameters,
            &mut self.island_manager,
            &mut self.broad_phase,
            &mut self.narrow_phase,
            &mut self.rigid_body_set,
            &mut self.collider_set,
            &mut self.impulse_joint_set,
            &mut self.multibody_joint_set,
            &mut self.ccd_solver,
            &(),
            &(),
        )
    }

    pub fn remove_collider(&mut self, handle: ColliderHandle) {
        self.collider_set.remove(
            handle,
            &mut self.island_manager,
            &mut self.rigid_body_set,
            false,
        );
    }
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

pub static PHYSICS_WORLDS: Lazy<Mutex<Vec<PhysicsWorld>>> = Lazy::new(|| Mutex::new(Vec::new()));

pub fn create_physics_world(callback: Callback) -> usize {
    let physics_world = PhysicsWorld {
        rapier: RapierWorld::new(),
        delta_time: 0.,
        callback,
        blocks: HashMap::new(),
        old_transforms: Vec::new(),
    };

    let mut physics_worlds = PHYSICS_WORLDS.lock();
    physics_worlds.push(physics_world);
    physics_worlds.len()
}

pub fn step_physics_world(index: usize, delta_time: f32, env: JNIEnv) -> i32 {
    let mut lock = PHYSICS_WORLDS.lock();
    let mut physics_world = match lock.get_mut(index) {
        Some(physics_world) => physics_world,
        None => return -1,
    };

    physics_world.delta_time += delta_time;

    while physics_world.delta_time >= STEP_TIME {
        physics_world.delta_time -= STEP_TIME;

        let mut wanted_blocks = HashSet::new();

        for (body_handle, body) in physics_world.rapier.rigid_body_set.iter() {
            if body.is_dynamic() && !body.is_sleeping() {
                let index = body_handle.into_raw_parts().0 as usize;
                *physics_world.old_transforms.get_mut(index).unwrap() = *body.position();

                let mut aabb: Option<AABB> = None;

                for collider in body
                    .colliders()
                    .iter()
                    .map(|handle| physics_world.rapier.collider_set.get(*handle).unwrap())
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
            &mut physics_world.rapier.collider_set,
        );

        physics_world.rapier.step();
    }

    0
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

    let position = *body.position();

    let body = physics_world.rapier.rigid_body_set.insert(body);
    physics_world.rapier.collider_set.insert_with_parent(
        shape,
        body,
        &mut physics_world.rapier.rigid_body_set,
    );

    let index = body.into_raw_parts().0 as usize;
    if index >= physics_world.old_transforms.len() {
        physics_world
            .old_transforms
            .resize(index as usize + 1, Isometry3::identity());
    }

    *physics_world.old_transforms.get_mut(index).unwrap() = position;

    Ok(body.into_raw_parts())
}

pub fn get_body_translation(index: usize, body: (u32, u32)) -> Result<Vector3<f32>, i32> {
    let lock = PHYSICS_WORLDS.lock();
    let physics_world = match lock.get(index) {
        Some(physics_world) => physics_world,
        None => return Err(-1),
    };

    match physics_world
        .rapier
        .rigid_body_set
        .get(RigidBodyHandle::from_raw_parts(body.0, body.1))
    {
        Some(body) => Ok(*body.translation()),
        None => Err(-1),
    }
}

pub fn get_render_transform(
    index: usize,
    body_handle: (u32, u32),
) -> Result<(Vector3<f32>, UnitQuaternion<f32>), i32> {
    let lock = PHYSICS_WORLDS.lock();
    let physics_world = match lock.get(index) {
        Some(physics_world) => physics_world,
        None => return Err(-1),
    };

    match physics_world
        .rapier
        .rigid_body_set
        .get(RigidBodyHandle::from_raw_parts(
            body_handle.0,
            body_handle.1,
        )) {
        Some(body) => {
            let delta = physics_world.delta_time / STEP_TIME;
            let old_position = physics_world.old_transforms[body_handle.0 as usize];
            let current_position = body.position();

            Ok(
                match old_position.try_lerp_slerp(current_position, delta, 0.0001) {
                    Some(position) => (position.translation.vector, position.rotation),
                    None => (
                        current_position.translation.vector,
                        current_position.rotation,
                    ),
                },
            )
        }
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
            assert!(self
                .blocks
                .insert(self.positions[i as usize], None)
                .is_none());
        }

        self.last_index = index;

        let pos = self.positions[index as usize];

        assert!(self
            .blocks
            .insert(pos, Some(make_colliders(pos, aabbs, self.collider_set)))
            .is_none());
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
