<?php
// This file is analogous to Utils.java and contains two static functions.
// No accented characters in comments or code.

class Utils {
    private static $temperaturaRandomIndicator = 0;
    private static $illuminationIntensityRandomIndicator = 0;
    
    // Private constructor to prevent instantiation.
    private function __construct() {
        throw new Exception("ERROR: No se puede instanciar esta clase.");
    }
    
    public static function manejarTemperaturaRandomIndicator() {
        $indicadorModulo = self::$temperaturaRandomIndicator % 8;
        $randomNumber = 0;
        if ($indicadorModulo == 0) {
            $randomNumber = rand(0, 5) + 25;
        } else if ($indicadorModulo == 1) {
            $randomNumber = rand(0, 5) + 20;
        } else if ($indicadorModulo == 2) {
            $randomNumber = rand(0, 5) + 15;
        } else if ($indicadorModulo == 3) {
            $randomNumber = rand(0, 14);
        } else if ($indicadorModulo == 4) {
            $randomNumber = rand(0, 5) + 15;
        } else if ($indicadorModulo == 5) {
            $randomNumber = rand(0, 5) + 20;
        } else if ($indicadorModulo == 6) {
            $randomNumber = rand(0, 5) + 25;
        } else if ($indicadorModulo == 7) {
            $randomNumber = rand(0, 14) + 31;
        }
        self::$temperaturaRandomIndicator++;
        return $randomNumber;
    }
    
    public static function manejarIlluminationIntensityRandomIndicator() {
        $indicadorModulo = self::$illuminationIntensityRandomIndicator % 12;
        $randomNumber = 0;
        if ($indicadorModulo == 0) {
            $randomNumber = rand(0, 300) + 1500;
        } else if ($indicadorModulo == 1) {
            $randomNumber = rand(0, 300) + 1800;
        } else if ($indicadorModulo == 2) {
            $randomNumber = rand(0, 300) + 2100;
        } else if ($indicadorModulo == 3) {
            $randomNumber = rand(0, 300) + 2400;
        } else if ($indicadorModulo == 4) {
            $randomNumber = rand(0, 300) + 2700;
        } else if ($indicadorModulo == 5) {
            $randomNumber = rand(0, 1499) + 3001;
        } else if ($indicadorModulo == 6) {
            $randomNumber = rand(0, 300) + 2700;
        } else if ($indicadorModulo == 7) {
            $randomNumber = rand(0, 300) + 2400;
        } else if ($indicadorModulo == 8) {
            $randomNumber = rand(0, 300) + 2100;
        } else if ($indicadorModulo == 9) {
            $randomNumber = rand(0, 300) + 1800;
        } else if ($indicadorModulo == 10) {
            $randomNumber = rand(0, 300) + 1500;
        } else if ($indicadorModulo == 11) {
            $randomNumber = rand(0, 1499);
        }
        self::$illuminationIntensityRandomIndicator++;
        return $randomNumber;
    }
}
