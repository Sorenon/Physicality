plugins {
    id("fr.stardustenterprises.rust.wrapper") version "2.1.0"
}

rust {
    command = "cargo"

//    environment = mapOf(Pair("RUSTUP_TOOLCHAIN", "nightly"))

    outputs = mapOf("" to System.mapLibraryName("mc_phys_jni"))

    outputDirectory = "META-INF/natives"

    profile = "debug"
}