<?php
// Utils.php
// Contains functions equivalent to Utils.java

namespace Oficina1Utils;

class Utils
{
    private static $temperaturaRandomIndicator = 0;
    private static $illuminationIntensityRandomIndicator = 0;

    // We replicate the same logic and random intervals as in Utils.java

    public static function manejarTemperaturaRandomIndicator()
    {
        $indicadorModulo = self::$temperaturaRandomIndicator % 8;
        $randomNumber = 0;

        if ($indicadorModulo === 0) {
            // random between 25 and 30
            $randomNumber = rand(25, 30);
        } else if ($indicadorModulo === 1) {
            // random between 20 and 25
            $randomNumber = rand(20, 25);
        } else if ($indicadorModulo === 2) {
            // random between 15 and 20
            $randomNumber = rand(15, 20);
        } else if ($indicadorModulo === 3) {
            // random between 0 and 14
            $randomNumber = rand(0, 14);
        } else if ($indicadorModulo === 4) {
            // random between 15 and 20
            $randomNumber = rand(15, 20);
        } else if ($indicadorModulo === 5) {
            // random between 20 and 25
            $randomNumber = rand(20, 25);
        } else if ($indicadorModulo === 6) {
            // random between 25 and 30
            $randomNumber = rand(25, 30);
        } else if ($indicadorModulo === 7) {
            // random between 31 and 45
            $randomNumber = rand(31, 45);
        }

        self::$temperaturaRandomIndicator++;
        return $randomNumber;
    }

    public static function manejarIlluminationIntensityRandomIndicator()
    {
        $indicadorModulo = self::$illuminationIntensityRandomIndicator % 12;
        $randomNumber = 0;

        switch ($indicadorModulo) {
            case 0:
                // random between 1500 and 1800
                $randomNumber = rand(1500, 1800);
                break;
            case 1:
                // random between 1800 and 2100
                $randomNumber = rand(1800, 2100);
                break;
            case 2:
                // random between 2100 and 2400
                $randomNumber = rand(2100, 2400);
                break;
            case 3:
                // random between 2400 and 2700
                $randomNumber = rand(2400, 2700);
                break;
            case 4:
                // random between 2700 and 3000
                $randomNumber = rand(2700, 3000);
                break;
            case 5:
                // random between 3001 and 4500
                $randomNumber = rand(3001, 4500);
                break;
            case 6:
                // random between 2700 and 3000
                $randomNumber = rand(2700, 3000);
                break;
            case 7:
                // random between 2400 and 2700
                $randomNumber = rand(2400, 2700);
                break;
            case 8:
                // random between 2100 and 2400
                $randomNumber = rand(2100, 2400);
                break;
            case 9:
                // random between 1800 and 2100
                $randomNumber = rand(1800, 2100);
                break;
            case 10:
                // random between 1500 and 1800
                $randomNumber = rand(1500, 1800);
                break;
            case 11:
                // random between 0 and 1499
                $randomNumber = rand(0, 1499);
                break;
        }

        self::$illuminationIntensityRandomIndicator++;
        return $randomNumber;
    }
}
