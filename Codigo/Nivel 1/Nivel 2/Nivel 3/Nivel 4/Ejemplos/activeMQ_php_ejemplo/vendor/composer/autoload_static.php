<?php

// autoload_static.php @generated by Composer

namespace Composer\Autoload;

class ComposerStaticInit0d6ac8357a3b5358a31c17046c1c2611
{
    public static $prefixLengthsPsr4 = array (
        'S' => 
        array (
            'Stomp\\' => 6,
        ),
    );

    public static $prefixDirsPsr4 = array (
        'Stomp\\' => 
        array (
            0 => __DIR__ . '/..' . '/stomp-php/stomp-php/src',
        ),
    );

    public static $classMap = array (
        'Composer\\InstalledVersions' => __DIR__ . '/..' . '/composer/InstalledVersions.php',
    );

    public static function getInitializer(ClassLoader $loader)
    {
        return \Closure::bind(function () use ($loader) {
            $loader->prefixLengthsPsr4 = ComposerStaticInit0d6ac8357a3b5358a31c17046c1c2611::$prefixLengthsPsr4;
            $loader->prefixDirsPsr4 = ComposerStaticInit0d6ac8357a3b5358a31c17046c1c2611::$prefixDirsPsr4;
            $loader->classMap = ComposerStaticInit0d6ac8357a3b5358a31c17046c1c2611::$classMap;

        }, null, ClassLoader::class);
    }
}
