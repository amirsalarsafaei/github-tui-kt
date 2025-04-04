{ pkgs, lib, config, inputs, ... }:

{
  languages = {
    kotlin.enable = true;
    java = {
      jdk.package = pkgs.jdk21_headless;
      enable = true;
      gradle.enable = true;
    };
  };

  packages = with pkgs; [
    kotlin-native
    sqlite
    sqlite.dev
    gcc
    glibc
    glibc.dev
    zlib
    zlib.dev
    openssl
    openssl.dev
    libxcrypt
    curl
    curl.dev
    gnumake
    cmake
    which
    pkg-config
    lldb
    libxcrypt-legacy
  ];

  env = {
    # nix-ld configuration
    LD_LIBRARY_PATH = with pkgs; lib.makeLibraryPath [
      glibc
      gcc.cc.lib
      zlib
      openssl
      curl
      sqlite
      libxcrypt
      libxcrypt-legacy
    ];

    SQLITE_LIB_PATH = "${lib.makeLibraryPath[pkgs.sqlite.out]}";


    PKG_CONFIG_PATH = "${pkgs.openssl.dev}/lib/pkgconfig:${pkgs.curl.dev}/lib/pkgconfig";
  };

  enterShell = ''
  '';
}