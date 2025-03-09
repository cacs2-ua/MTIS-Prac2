<?php
// Oficina1.php
// PHP-based replacement for the Java "Oficina1" using stomp-php 5.1.2 and react/event-loop
//
// Make sure to run "composer install" so that the following classes are available:
//   - Stomp\Client
//   - Stomp\StatefulStomp
//   - React\EventLoop\Factory
// Then run this script via: php Oficina1.php

require_once __DIR__ . '/vendor/autoload.php';
require_once __DIR__ . '/Utils.php';

use Stomp\Client;
use Stomp\StatefulStomp;
use Stomp\Transport\Message;
use React\EventLoop\Factory;
use Oficina1Utils\Utils;

class Oficina1
{
    // Temperature variables
    private $temperature;
    private $heatSystemActivated = false;
    private $coldSystemActivated = false;

    // Illumination variables
    private $illuminationIntensity;
    private $increaseIntensityIlluminationRegulatorActivated = false;
    private $decreaseIntensityIlluminationRegulatorActivated = false;

    // Constants analogous to Java
    const TEMPERATURE_DIFFERENCE = 2;
    const HEAT_SYSTEM_STOP_TEMPERATURE = 23;
    const COLD_SYSTEM_STOP_TEMPERATURE = 23;

    const ILLUMINATION_INTENSITY_DIFFERENCE = 200;
    const INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY = 2300;
    const DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY = 2300;

    // Topics
    private $topicLecturasTemperature = '/topic/lecturas_temperaturas_oficina1';
    private $topicActuadorTemperature = '/topic/actuador_temperatura_oficina1';
    private $topicLecturasIllumination = '/topic/lecturas_iluminacion_oficina1';
    private $topicActuadorIllumination = '/topic/actuador_iluminacion_oficina1';

    /** @var StatefulStomp */
    private $stomp;
    /** @var \React\EventLoop\LoopInterface */
    private $loop;

    public function __construct()
    {
        // same initial values as in Oficina1.java
        $this->temperature = 0;
        $this->illuminationIntensity = 2300;
    }

    public function start()
    {
        echo "Office1 Starting... \n";

        $this->loop = Factory::create();

        // 1) Create the stomp client
        $client = new Client('tcp://localhost:61613');
        $client->setLogin('mtis', 'mtis'); // user + pass
        $client->connect();
        // 2) Wrap it in a StatefulStomp
        $this->stomp = new StatefulStomp($client);

        // 3) Subscribe to the actuator topics
        //    We'll poll them in the event loop to keep it asynchronous
        $this->stomp->subscribe($this->topicActuadorTemperature, null, 'auto');
        $this->stomp->subscribe($this->topicActuadorIllumination, null, 'auto');

        // Continuously read frames in a small periodic timer
        $this->loop->addPeriodicTimer(0.1, function () {
            if ($frame = $this->stomp->read()) {
                // We have a message
                $this->onMessage($frame);
            }
        });

        // Define temperature task
        $temperatureTask = function () {
            // If no system is active, randomize temperature
            if (!$this->coldSystemActivated && !$this->heatSystemActivated) {
                $this->temperature = Utils::manejarTemperaturaRandomIndicator();
            }
            if ($this->heatSystemActivated) {
                $this->manageHeatSystem();
            } elseif ($this->coldSystemActivated) {
                $this->manageColdSystem();
            } else {
                // Just send the current temperature
                $this->sendTemperatureMessage();
            }
            $this->printOwnTemperatureInformation();
        };

        // Define illumination task
        $illuminationTask = function () {
            // If no regulator is active, randomize illumination
            if (!$this->increaseIntensityIlluminationRegulatorActivated && !$this->decreaseIntensityIlluminationRegulatorActivated) {
                $this->illuminationIntensity = Utils::manejarIlluminationIntensityRandomIndicator();
            }
            if ($this->increaseIntensityIlluminationRegulatorActivated) {
                $this->manageIncreaseIlluminationRegulator();
            } elseif ($this->decreaseIntensityIlluminationRegulatorActivated) {
                $this->manageDecreaseIlluminationRegulator();
            } else {
                // Just send the current illumination
                $this->sendIlluminationMessage();
            }
            $this->printOwnIlluminationInformation();
        };

        // Execute tasks immediately so messages appear without delay
        $temperatureTask();
        $illuminationTask();

        // Schedule temperature task every 2 seconds
        $this->loop->addPeriodicTimer(2.0, $temperatureTask);

        // Schedule illumination task every 2 seconds
        $this->loop->addPeriodicTimer(2.0, $illuminationTask);

        // Run the React event loop
        $this->loop->run();
    }

    // ---- JMS-like onMessage (actually a frame read) ----
    private function onMessage($frame)
    {
        $body = $frame->getBody();
        if (empty($body)) {
            echo "Received empty message.\n";
            return;
        }
        // parse as JSON
        $data = json_decode($body, true);
        if ($data === null) {
            echo "Got invalid JSON.\n";
            return;
        }

        // If there's a "temperature" field, handle temperature flags
        if (array_key_exists('temperature', $data)) {
            $this->manageTemperatureFlags($data);
            $this->printReceivedTemperatureInformation($data);
        }
        // If there's an "illumination_intensity" field, handle illumination
        if (array_key_exists('illumination_intensity', $data)) {
            $this->manageIlluminationFlags($data);
            $this->printReceivedIlluminationInformation($data);
        }
    }

    // ---- Manage temperature flags ----
    private function manageTemperatureFlags($json)
    {
        if (isset($json['activate_cold_system']) && $json['activate_cold_system'] === true) {
            $this->coldSystemActivated = true;
        }
        if (isset($json['stop_cold_system']) && $json['stop_cold_system'] === true) {
            $this->coldSystemActivated = false;
        }
        if (isset($json['activate_heat_system']) && $json['activate_heat_system'] === true) {
            $this->heatSystemActivated = true;
        }
        if (isset($json['stop_heat_system']) && $json['stop_heat_system'] === true) {
            $this->heatSystemActivated = false;
        }
    }

    // ---- Manage illumination flags ----
    private function manageIlluminationFlags($json)
    {
        if (isset($json['activate_increase_intensity_illumination_regulator']) 
            && $json['activate_increase_intensity_illumination_regulator'] === true) {
            $this->increaseIntensityIlluminationRegulatorActivated = true;
        }
        if (isset($json['stop_increase_intensity_illumination_regulator']) 
            && $json['stop_increase_intensity_illumination_regulator'] === true) {
            $this->increaseIntensityIlluminationRegulatorActivated = false;
        }
        if (isset($json['activate_decrease_intensity_illumination_regulator']) 
            && $json['activate_decrease_intensity_illumination_regulator'] === true) {
            $this->decreaseIntensityIlluminationRegulatorActivated = true;
        }
        if (isset($json['stop_decrease_intensity_illumination_regulator']) 
            && $json['stop_decrease_intensity_illumination_regulator'] === true) {
            $this->decreaseIntensityIlluminationRegulatorActivated = false;
        }
    }

    // ---- Methods for incrementing/decrementing temperature ----
    private function incrementTemperature()
    {
        $this->temperature += self::TEMPERATURE_DIFFERENCE;
    }

    private function decrementTemperature()
    {
        $this->temperature -= self::TEMPERATURE_DIFFERENCE;
    }

    // ---- Send or manage temperature messages ----
    private function sendTemperatureMessage()
    {
        $payloadArray = [
            "office"                => "office1",
            "temperature"           => $this->temperature,
            "cold_system_activated" => $this->coldSystemActivated,
            "heat_system_activated" => $this->heatSystemActivated
        ];
        $payloadJson = json_encode($payloadArray);
        $msg = new Message($payloadJson, ["content-type" => "application/json"]);
        $this->stomp->send($this->topicLecturasTemperature, $msg);
    }

    public function manageColdSystem()
    {
        if (!$this->coldSystemActivated) {
            $this->coldSystemActivated = true;
        }
        $this->decrementTemperature();

        $payloadArray = [
            "office"                => "office1",
            "temperature"           => $this->temperature,
            "cold_system_activated" => $this->coldSystemActivated,
            "heat_system_activated" => $this->heatSystemActivated
        ];
        $payloadJson = json_encode($payloadArray);
        $msg = new Message($payloadJson, ["content-type" => "application/json"]);
        $this->stomp->send($this->topicLecturasTemperature, $msg);

        if ($this->temperature <= self::COLD_SYSTEM_STOP_TEMPERATURE) {
            $this->coldSystemActivated = false;
        }
    }

    public function manageHeatSystem()
    {
        if (!$this->heatSystemActivated) {
            $this->heatSystemActivated = true;
        }
        $this->incrementTemperature();

        $payloadArray = [
            "office"                => "office1",
            "temperature"           => $this->temperature,
            "cold_system_activated" => $this->coldSystemActivated,
            "heat_system_activated" => $this->heatSystemActivated
        ];
        $payloadJson = json_encode($payloadArray);
        $msg = new Message($payloadJson, ["content-type" => "application/json"]);
        $this->stomp->send($this->topicLecturasTemperature, $msg);

        if ($this->temperature >= self::HEAT_SYSTEM_STOP_TEMPERATURE) {
            $this->heatSystemActivated = false;
        }
    }

    public function printOwnTemperatureInformation()
    {
        echo "Temperature Sensor: " . $this->temperature . "C\n";
        if ($this->coldSystemActivated) {
            echo "Temperature Activator: Cold System activated.\n";
        }
        if ($this->heatSystemActivated) {
            echo "Temperature Activator: Heat System activated.\n";
        }
    }

    public function printReceivedTemperatureInformation($json)
    {
        if (isset($json['activate_cold_system']) && $json['activate_cold_system'] === true) {
            echo "Temperature Activator: Temperature has exceeded 30C. Activating Cold System...\n";
        }
        if (isset($json['stop_cold_system']) && $json['stop_cold_system'] === true) {
            echo "Temperature Activator: Temperature has reached 23C or less. Stopping Cold System...\n";
        }
        if (isset($json['activate_heat_system']) && $json['activate_heat_system'] === true) {
            echo "Temperature Activator: Temperature is below 15C. Activating Heat System...\n";
        }
        if (isset($json['stop_heat_system']) && $json['stop_heat_system'] === true) {
            echo "Temperature Activator: Temperature has reached 23C or more. Stopping Heat System...\n";
        }
    }

    // ---- Methods for illumination ----
    private function sendIlluminationMessage()
    {
        $payloadArray = [
            "office"                                 => "office1",
            "illumination_intensity"                 => $this->illuminationIntensity,
            "increase_intensity_regulator_activated" => $this->increaseIntensityIlluminationRegulatorActivated,
            "decrease_intensity_regulator_activated" => $this->decreaseIntensityIlluminationRegulatorActivated
        ];
        $payloadJson = json_encode($payloadArray);
        $msg = new Message($payloadJson, ["content-type" => "application/json"]);
        $this->stomp->send($this->topicLecturasIllumination, $msg);
    }

    public function manageIncreaseIlluminationRegulator()
    {
        if (!$this->increaseIntensityIlluminationRegulatorActivated) {
            $this->increaseIntensityIlluminationRegulatorActivated = true;
        }
        $this->illuminationIntensity += self::ILLUMINATION_INTENSITY_DIFFERENCE;

        $payloadArray = [
            "office"                                 => "office1",
            "illumination_intensity"                 => $this->illuminationIntensity,
            "increase_intensity_regulator_activated" => $this->increaseIntensityIlluminationRegulatorActivated,
            "decrease_intensity_regulator_activated" => $this->decreaseIntensityIlluminationRegulatorActivated
        ];
        $payloadJson = json_encode($payloadArray);
        $msg = new Message($payloadJson, ["content-type" => "application/json"]);
        $this->stomp->send($this->topicLecturasIllumination, $msg);

        if ($this->illuminationIntensity >= self::INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY) {
            $this->increaseIntensityIlluminationRegulatorActivated = false;
        }
    }

    public function manageDecreaseIlluminationRegulator()
    {
        if (!$this->decreaseIntensityIlluminationRegulatorActivated) {
            $this->decreaseIntensityIlluminationRegulatorActivated = true;
        }
        $this->illuminationIntensity -= self::ILLUMINATION_INTENSITY_DIFFERENCE;

        $payloadArray = [
            "office"                                 => "office1",
            "illumination_intensity"                 => $this->illuminationIntensity,
            "increase_intensity_regulator_activated" => $this->increaseIntensityIlluminationRegulatorActivated,
            "decrease_intensity_regulator_activated" => $this->decreaseIntensityIlluminationRegulatorActivated
        ];
        $payloadJson = json_encode($payloadArray);
        $msg = new Message($payloadJson, ["content-type" => "application/json"]);
        $this->stomp->send($this->topicLecturasIllumination, $msg);

        if ($this->illuminationIntensity <= self::DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY) {
            $this->decreaseIntensityIlluminationRegulatorActivated = false;
        }
    }

    public function printOwnIlluminationInformation()
    {
        echo "Illumination Sensor: " . $this->illuminationIntensity . " lumens\n";
        if ($this->increaseIntensityIlluminationRegulatorActivated) {
            echo "Illumination Activator: Increase Intensity Regulator activated.\n";
        }
        if ($this->decreaseIntensityIlluminationRegulatorActivated) {
            echo "Illumination Activator: Decrease Intensity Regulator activated.\n";
        }
    }

    public function printReceivedIlluminationInformation($json)
    {
        if (isset($json['activate_increase_intensity_illumination_regulator'])
            && $json['activate_increase_intensity_illumination_regulator'] === true) {
            echo "Illumination Activator: Illumination intensity is below threshold (below 1500 lumens). Activating Increase Intensity Regulator...\n";
        }
        if (isset($json['stop_increase_intensity_illumination_regulator'])
            && $json['stop_increase_intensity_illumination_regulator'] === true) {
            echo "Illumination Activator: Illumination intensity has reached desired level (around 2300 lumens). Stopping Increase Intensity Regulator...\n";
        }
        if (isset($json['activate_decrease_intensity_illumination_regulator'])
            && $json['activate_decrease_intensity_illumination_regulator'] === true) {
            echo "Illumination Activator: Illumination intensity is above threshold (above 3000 lumens). Activating Decrease Intensity Regulator...\n";
        }
        if (isset($json['stop_decrease_intensity_illumination_regulator'])
            && $json['stop_decrease_intensity_illumination_regulator'] === true) {
            echo "Illumination Activator: Illumination intensity has reached desired level (around 2300 lumens). Stopping Decrease Intensity Regulator...\n";
        }
    }
}

// ---- Entry point ---- 
$oficina = new Oficina1();
$oficina->start();
