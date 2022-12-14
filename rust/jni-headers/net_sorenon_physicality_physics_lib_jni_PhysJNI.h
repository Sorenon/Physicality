/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class net_sorenon_physicality_physics_lib_jni_PhysJNI */

#ifndef _Included_net_sorenon_physicality_physics_lib_jni_PhysJNI
#define _Included_net_sorenon_physicality_physics_lib_jni_PhysJNI
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     net_sorenon_physicality_physics_lib_jni_PhysJNI
 * Method:    debugRender
 * Signature: (JLnet/sorenon/physicality/physics_lib/jni/DebugRenderCallback;)I
 */
JNIEXPORT jint JNICALL Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_debugRender
  (JNIEnv *, jclass, jlong, jobject);

/*
 * Class:     net_sorenon_physicality_physics_lib_jni_PhysJNI
 * Method:    createPhysicsWorld
 * Signature: (Lnet/minecraft/world/level/Level;Lnet/sorenon/physicality/physics_lib/jni/PhysicsCallback;)J
 */
JNIEXPORT jlong JNICALL Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_createPhysicsWorld
  (JNIEnv *, jclass, jobject, jobject);

/*
 * Class:     net_sorenon_physicality_physics_lib_jni_PhysJNI
 * Method:    step
 * Signature: (JF)I
 */
JNIEXPORT jint JNICALL Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_step
  (JNIEnv *, jclass, jlong, jfloat);

/*
 * Class:     net_sorenon_physicality_physics_lib_jni_PhysJNI
 * Method:    addPhysicsBody
 * Signature: (JFFFJ)I
 */
JNIEXPORT jint JNICALL Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_addPhysicsBody
  (JNIEnv *, jclass, jlong, jfloat, jfloat, jfloat, jlong);

/*
 * Class:     net_sorenon_physicality_physics_lib_jni_PhysJNI
 * Method:    addCuboid
 * Signature: (JFFFFFFFJIJ)I
 */
JNIEXPORT jint JNICALL Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_addCuboid
  (JNIEnv *, jclass, jlong, jfloat, jfloat, jfloat, jfloat, jfloat, jfloat, jfloat, jlong, jint, jlong);

/*
 * Class:     net_sorenon_physicality_physics_lib_jni_PhysJNI
 * Method:    getBodyPosition
 * Signature: (JJLorg/joml/Vector3f;)I
 */
JNIEXPORT jint JNICALL Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_getBodyPosition
  (JNIEnv *, jclass, jlong, jlong, jobject);

/*
 * Class:     net_sorenon_physicality_physics_lib_jni_PhysJNI
 * Method:    getBodyRenderTransform
 * Signature: (JJLorg/joml/Vector3f;Lorg/joml/Quaternionf;)I
 */
JNIEXPORT jint JNICALL Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_getBodyRenderTransform
  (JNIEnv *, jclass, jlong, jlong, jobject, jobject);

/*
 * Class:     net_sorenon_physicality_physics_lib_jni_PhysJNI
 * Method:    blockUpdated
 * Signature: (JIIIJJ)I
 */
JNIEXPORT jint JNICALL Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_blockUpdated
  (JNIEnv *, jclass, jlong, jint, jint, jint, jlong, jlong);

/*
 * Class:     net_sorenon_physicality_physics_lib_jni_PhysJNI
 * Method:    sendBlockInfo
 * Signature: (JIJJ)I
 */
JNIEXPORT jint JNICALL Java_net_sorenon_physicality_physics_1lib_jni_PhysJNI_sendBlockInfo
  (JNIEnv *, jclass, jlong, jint, jlong, jlong);

#ifdef __cplusplus
}
#endif
#endif
