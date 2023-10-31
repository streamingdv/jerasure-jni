//
// Created by Florian on 09.08.2019.
//
#include "com_grill_jerasure_CauchyReedSolomonCodec.h"

#include <jni.h>
#include <stdlib.h>
#include <cauchy.h>
#include <jerasure.h>
#include <map>

using namespace std;

map<int, int *> matrix_pointers;

int create_hash(int k, int m, int w) {
    int result = 1;
    result = 31 * result + std::hash<int>()(k);
    result = 31 * result + std::hash<int>()(m);
    result = 31 * result + std::hash<int>()(w);
    return result;
}

int * get_or_create_matrix_pointer(int k, int m, int w) {
    int hash = create_hash(k, m, w);
    int * result = matrix_pointers[hash];
    if(result == nullptr) {
        result = cauchy_original_coding_matrix(k, m, w);
        if(matrix_pointers.size() <= 200) {
            matrix_pointers[hash] = result;
        }
    }
    return result;
}

// Convert a java byte[][] array to a native char[][] array
char **convert_to_native_char_array(JNIEnv *env, jobjectArray matrix, size_t size) {
    jsize rowNum = env->GetArrayLength(matrix);
    char **result = new char *[rowNum];

    for (int row = 0; row < rowNum; ++row) {
        auto array = (jbyteArray) (env->GetObjectArrayElement(matrix, row));
        if (array != nullptr) {
            result[row] = (char *) (env->GetByteArrayElements(array, nullptr));
        } else {
            result[row] = (char *) malloc(size);
        }
    }
    return result;
}

void copy_decoded_data(JNIEnv *env, jobjectArray data,
                     const jint *nativeErasures, jsize erasuresLength, jsize dataSize,
                     char **decodedData) {
    int realSizeIndex = 0;
    for (int index = 0; index < erasuresLength; ++index) {
        jint value = nativeErasures[index];
        if (value >= 0 && value < dataSize) {
            auto array = (jbyteArray) (env->GetObjectArrayElement(
                    data, value));
            jsize data_len = env->GetArrayLength(array);
            env->SetByteArrayRegion(array, 0, data_len,
                                    (jbyte *) (decodedData[value]));
            ++realSizeIndex;
        }
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_grill_jerasure_CauchyReedSolomonCodec_createCauchyMatrix
        (JNIEnv *env, jobject, jint k, jint m, jint w) {
    int *matrix = cauchy_original_coding_matrix(k, m, w);
    if (matrix != nullptr) {
        return (long) matrix;
    } else {
        jclass jClass = env->FindClass("java/lang/Exception");
        env->ThrowNew(jClass, "Not enough free memory to complete");
    }
    return 0L;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_grill_jerasure_CauchyReedSolomonCodec_cleanUpCauchyMatrix(JNIEnv *env, jclass) {
    map<int, int *>::iterator key_value;
    for(key_value = matrix_pointers.begin(); key_value != matrix_pointers.end(); key_value++){
        free(key_value->second);
    }
    matrix_pointers.clear();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_grill_jerasure_CauchyReedSolomonCodec_jerasureDecode(JNIEnv *env, jclass,
                                                              jint k, jint m, jint w,
                                                              jint row_k_ones,
                                                              jintArray erasures_,
                                                              jobjectArray data_ptrs,
                                                              jobjectArray coding_ptrs,
                                                              jint size) {
    int *matrix = get_or_create_matrix_pointer(k, m, w);
    if (matrix == nullptr) {
        jclass jClass = env->FindClass("java/lang/Exception");
        env->ThrowNew(jClass, "Could not create cauchy matrix");
        return JNI_FALSE;
    }
    char **data = convert_to_native_char_array(env, data_ptrs, static_cast<size_t>(size));
    char **nativeCoding = convert_to_native_char_array(env, coding_ptrs, static_cast<size_t>(size));
    int *erasures = (int *) env->GetIntArrayElements(erasures_, nullptr);
    jsize erasuresLength = env->GetArrayLength(erasures_);
    int res = jerasure_matrix_decode(k, m, w, matrix, row_k_ones, erasures, data, nativeCoding,
                                     size);
    if ((res < 0)) {
        env->ReleaseIntArrayElements(erasures_, (jint *) erasures, 0);
        return JNI_FALSE;
    }
    copy_decoded_data(env, data_ptrs, (jint *) erasures, erasuresLength, k, data);
    env->ReleaseIntArrayElements(erasures_, (jint *) erasures, 0);
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_grill_jerasure_CauchyReedSolomonCodec_getSecret(JNIEnv *env, jclass) {
    const char *test = "ab453940ca568eff";
    return env->NewStringUTF(test);
}