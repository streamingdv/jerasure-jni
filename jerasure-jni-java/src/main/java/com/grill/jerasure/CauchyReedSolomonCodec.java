package com.grill.jerasure;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class CauchyReedSolomonCodec {
    private final int dataBlockNum;
    private final int codingBlockNum;
    private final int wordSize;
    private final int packetSize;
    private final long matrix;

    private static final Map<Integer, CauchyReedSolomonCodec> cauchyReedSolomonCodecCache = new HashMap<>();

    private static final int MAX_CACHE_SIZE = 200;

    private CauchyReedSolomonCodec(int dataBlockNum, int codingBlockNum, int packetSize) {
        checkArgument(dataBlockNum > 0);
        checkArgument(codingBlockNum > 0);
        checkArgument(packetSize > 0);

        this.dataBlockNum = dataBlockNum;
        this.codingBlockNum = codingBlockNum;
        this.wordSize = 8;
        this.packetSize = packetSize;
        this.matrix = this.createCauchyMatrix(dataBlockNum,
                codingBlockNum, this.wordSize);
    }

    public static CauchyReedSolomonCodec getCauchyReedSolomonCodec(int dataBlockNum, int codingBlockNum, int packetSize) {
        int hash = Objects.hash(dataBlockNum, codingBlockNum, packetSize);
        CauchyReedSolomonCodec cauchyReedSolomonCodec = cauchyReedSolomonCodecCache.get(hash);
        if (cauchyReedSolomonCodec == null) {
            cauchyReedSolomonCodec = new CauchyReedSolomonCodec(dataBlockNum, codingBlockNum, packetSize);
            if (cauchyReedSolomonCodecCache.size() <= MAX_CACHE_SIZE) {
                cauchyReedSolomonCodecCache.put(hash, cauchyReedSolomonCodec);
            }
        }
        return cauchyReedSolomonCodec;
    }

    public static void clearCauchyReedSolomonCodecCache() {
        cleanUpCauchyMatrix();
        cauchyReedSolomonCodecCache.clear();
    }

    public boolean decode(byte[][] data, byte[][] coding, int[] erasures) {
        return jerasureDecode(this.dataBlockNum, this.codingBlockNum, this.wordSize,  0, erasures, data, coding, this.packetSize);
    }

    public static String getSecretValue() {
        return getSecret();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        CauchyReedSolomonCodec that = (CauchyReedSolomonCodec) o;
        return this.dataBlockNum == that.dataBlockNum &&
                this.codingBlockNum == that.codingBlockNum &&
                this.packetSize == that.packetSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.dataBlockNum, this.codingBlockNum, this.packetSize);
    }

    private static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Creates a Cauchy matrix over GF(2^w).
     *
     * @param k The column number
     * @param m The row number
     * @param w The word size, used to define the finite field
     * @return The generated Cauchy matrix
     */
    private native long createCauchyMatrix(int k, int m, int w);

    private static native boolean jerasureDecode(int k, int m, int w,
                                                 int row_k_ones, int[] erasures,
                                                 byte[][] data_ptrs, byte[][] coding_ptrs, int size);

    private static native void cleanUpCauchyMatrix();

    private static native String getSecret();

    /************************/
    /*** load lib methods ***/
    /************************/

    public static void loadNative(File directory) throws IOException {
        loadNative(directory, true);
    }

    public static void loadNative(File directory, boolean allowArm) throws IOException {
        String nativeLibraryName = getNativeLibraryName(allowArm);
        InputStream source = CauchyReedSolomonCodec.class.getResourceAsStream("/native-binaries/" + nativeLibraryName);
        if (source == null) {
            throw new IOException("Could not find native library " + nativeLibraryName);
        }

        Path destination = directory.toPath().resolve(nativeLibraryName);
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (AccessDeniedException ignored) {
            // The file already exists, or we don't have permission to write to the directory
        }
        System.load(new File(directory, nativeLibraryName).getAbsolutePath());
    }

    /**
     * Extract the native library and load it
     *
     * @throws IOException          In case an error occurs while extracting the native library
     * @throws UnsatisfiedLinkError In case the native libraries fail to load
     */
    public static void setupWithTemporaryFolder() throws IOException {
        File temporaryDir = Files.createTempDirectory("jerasure-jni").toFile();
        temporaryDir.deleteOnExit();

        try {
            loadNative(temporaryDir);
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();

            // Try without ARM support
            loadNative(temporaryDir, false);
        }
    }

    /***********************/
    /*** private methods ***/
    /***********************/

    private static String getNativeLibraryName(boolean allowArm) {
        String bitnessArch = System.getProperty("os.arch").toLowerCase();
        String bitnessDataModel = System.getProperty("sun.arch.data.model", null);

        boolean is64bit = bitnessArch.contains("64") || (bitnessDataModel != null && bitnessDataModel.contains("64"));
        String arch = bitnessArch.startsWith("aarch") && allowArm ? "arm" : "";

        if (is64bit) {
            String library64 = processLibraryName("jerasure-jni-native-" + arch + "64");
            if (hasResource("/native-binaries/" + library64)) {
                return library64;
            }
        } else {
            String library32 = processLibraryName("jerasure-jni-native-" + arch + "32");
            if (hasResource("/native-binaries/" + library32)) {
                return library32;
            }
        }

        String library = processLibraryName("jerasure-jni-native");
        if (!hasResource("/native-binaries/" + library)) {
            throw new NoSuchElementException("No binary for the current system found, even after trying bit neutral names");
        } else {
            return library;
        }
    }

    private static String processLibraryName(String library) {
        String systemName = System.getProperty("os.name", "bare-metal?").toLowerCase();

        if (systemName.contains("nux") || systemName.contains("nix")) {
            return "lib" + library + ".so";
        } else if (systemName.contains("mac")) {
            return "lib" + library + ".dylib";
        } else if (systemName.contains("windows")) {
            return library + ".dll";
        } else {
            throw new NoSuchElementException("No native library for system " + systemName);
        }
    }

    private static boolean hasResource(String resource) {
        return CauchyReedSolomonCodec.class.getResource(resource) != null;
    }
}
