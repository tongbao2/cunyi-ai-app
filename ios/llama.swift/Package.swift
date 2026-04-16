// Package.swift — llama.cpp C API Swift 绑定（精简版）
// llama.cpp C 源码从 git clone，暴露 GGUF 推理关键接口
import PackageDescription

let package = Package(
    name: "llama",
    platforms: [.iOS(.v16)],
    products: [
        .library(name: "llama", type: .static, targets: ["llama"])
    ],
    targets: [
        .target(
            name: "llama",
            dependencies: [],
            cxxSettings: [
                .headerSearchPath("llama.cpp"),
                .define("GGML_NO_LAPACK"),
                .define("GGML_USE_METAL"),
                .define("LLAMA_MAX_THREADS=4"),
                .cxxLanguageStandard(.cxx17)
            ],
            settings: [
                .compileFlags(["-fno-objc-arc"]),
                .linkSettings([
                    .linkedLibrary("c++", .thatDynamic),
                    .linkedLibrary("m", .thatMember),
                    .linkedLibrary("metal", .thatMember),
                    .linkedLibrary("Foundation", .thatMember),
                    .linkedLibrary("Accelerate", .thatMember),
                ])
            ]
        )
    ]
)
