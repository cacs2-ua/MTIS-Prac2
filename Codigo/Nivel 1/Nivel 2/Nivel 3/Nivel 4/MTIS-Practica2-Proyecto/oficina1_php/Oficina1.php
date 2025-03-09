<?php
// No accented characters in comments or code.
// This file is analogous to Oficina1.java

require __DIR__ . '/vendor/autoload.php';
require __DIR__ . '/Utils.php';

use React\EventLoop\Factory;
use Stomp\Client;
use Stomp\Transport\Message;

class Oficina1 {
    // Temperature variables
    private $temperature = 0;
    private $heatSystemActivated = false;
    private $coldSystemActivated = false;
    
    // Illumination variables
    private $illuminationIntensity = 2300;
    private $increaseIntensityIlluminationRegulatorActivated = false;
    private $decreaseIntensityIlluminationRegulatorActivated = false;
    
    // Constants
    const TEMPERATURE_DIFFERENCE = 2;
    const HEAT_SYSTEM_STOP_TEMPERATURE = 23;
    const COLD_SYSTEM_STOP_TEMPERATURE = 23;
    
    const ILLUMINATION_INTENSITY_DIFFERENCE = 200;
    const INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY = 2300;
    const DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY = 2300;
    
    private $client;
    private $loop;
    
    public function __construct($client, $loop) {
        $this->client = $client;
        $this->loop = $loop;
    }
    
    // Getter and setter methods
    public function setTemperature($temp) {
        $this->temperature = $temp;
    }
    public function getTemperature() {
        return $this->temperature;
    }
    public function setColdSystemActivated($value) {
        $this->coldSystemActivated = $value;
    }
    public function isColdSystemActivated() {
        return $this->coldSystemActivated;
    }
    public function setHeatSystemActivated($value) {
        $this->heatSystemActivated = $value;
    }
    public function isHeatSystemActivated() {
        return $this->heatSystemActivated;
    }
    public function setIlluminationIntensity($value) {
        $this->illuminationIntensity = $value;
    }
    public function getIlluminationIntensity() {
        return $this->illuminationIntensity;
    }
    
    public function incrementTemperature() {
        $this->temperature += self::TEMPERATURE_DIFFERENCE;
    }
    public function decrementTemperature() {
        $this->temperature -= self::TEMPERATURE_DIFFERENCE;
    }
    
    // Sends a temperature reading message
    public function sendTemperatureMessage() {
        $payload = json_encode([
            "office" => "office1",
            "temperature" => $this->temperature,
            "cold_system_activated" => $this->coldSystemActivated,
            "heat_system_activated" => $this->heatSystemActivated
        ]);
        $message = new Message($payload);
        $this->client->send("/topic/lecturas_temperaturas_oficina1", $message);
    }
    
    // Manages the cold system: decrements temperature, sends message, deactivates if condition met.
    public function manageColdSystem() {
        if (!$this->coldSystemActivated) {
            $this->setColdSystemActivated(true);
        }
        $this->decrementTemperature();
        $payload = json_encode([
            "office" => "office1",
            "temperature" => $this->temperature,
            "cold_system_activated" => $this->coldSystemActivated,
            "heat_system_activated" => $this->heatSystemActivated
        ]);
        $message = new Message($payload);
        $this->client->send("/topic/lecturas_temperaturas_oficina1", $message);
        if ($this->temperature <= self::COLD_SYSTEM_STOP_TEMPERATURE) {
            $this->setColdSystemActivated(false);
        }
    }
    
    // Manages the heat system: increments temperature, sends message, deactivates if condition met.
    public function manageHeatSystem() {
        if (!$this->heatSystemActivated) {
            $this->setHeatSystemActivated(true);
        }
        $this->incrementTemperature();
        $payload = json_encode([
            "office" => "office1",
            "temperature" => $this->temperature,
            "cold_system_activated" => $this->coldSystemActivated,
            "heat_system_activated" => $this->heatSystemActivated
        ]);
        $message = new Message($payload);
        $this->client->send("/topic/lecturas_temperaturas_oficina1", $message);
        if ($this->temperature >= self::HEAT_SYSTEM_STOP_TEMPERATURE) {
            $this->setHeatSystemActivated(false);
        }
    }
    
    // Temperature flag management methods
    public function getActivateColdSystemFlag($text) {
        $json = json_decode($text, true);
        return isset($json["activate_cold_system"]) ? $json["activate_cold_system"] : null;
    }
    public function getStopColdSystemFlag($text) {
        $json = json_decode($text, true);
        return isset($json["stop_cold_system"]) ? $json["stop_cold_system"] : null;
    }
    public function getActivateHeatSystemFlag($text) {
        $json = json_decode($text, true);
        return isset($json["activate_heat_system"]) ? $json["activate_heat_system"] : null;
    }
    public function getStopHeatSystemFlag($text) {
        $json = json_decode($text, true);
        return isset($json["stop_heat_system"]) ? $json["stop_heat_system"] : null;
    }
    public function manageTemperatureFlags($text) {
        $activateColdSystem = $this->getActivateColdSystemFlag($text);
        $stopColdSystem = $this->getStopColdSystemFlag($text);
        $activateHeatSystem = $this->getActivateHeatSystemFlag($text);
        $stopHeatSystem = $this->getStopHeatSystemFlag($text);
        
        if ($activateColdSystem !== null && $activateColdSystem) {
            $this->setColdSystemActivated(true);
        }
        if ($stopColdSystem !== null && $stopColdSystem) {
            $this->setColdSystemActivated(false);
        }
        if ($activateHeatSystem !== null && $activateHeatSystem) {
            $this->setHeatSystemActivated(true);
        }
        if ($stopHeatSystem !== null && $stopHeatSystem) {
            $this->setHeatSystemActivated(false);
        }
    }
    public function printOwnTemperatureInformation() {
        echo "Temperature Sensor: " . $this->temperature . "C\n";
        if ($this->coldSystemActivated) {
            echo "Temperature Activator: Cold System activated.\n";
        }
        if ($this->heatSystemActivated) {
            echo "Temperature Activator: Heat System activated.\n";
        }
    }
    
    // Illumination methods
    public function sendIlluminationMessage() {
        $payload = json_encode([
            "office" => "office1",
            "illumination_intensity" => $this->illuminationIntensity,
            "increase_intensity_regulator_activated" => $this->increaseIntensityIlluminationRegulatorActivated,
            "decrease_intensity_regulator_activated" => $this->decreaseIntensityIlluminationRegulatorActivated
        ]);
        $message = new Message($payload);
        $this->client->send("/topic/lecturas_iluminacion_oficina1", $message);
    }
    
    public function manageIncreaseIlluminationRegulator() {
        if (!$this->increaseIntensityIlluminationRegulatorActivated) {
            $this->increaseIntensityIlluminationRegulatorActivated = true;
        }
        $this->illuminationIntensity += self::ILLUMINATION_INTENSITY_DIFFERENCE;
        $payload = json_encode([
            "office" => "office1",
            "illumination_intensity" => $this->illuminationIntensity,
            "increase_intensity_regulator_activated" => $this->increaseIntensityIlluminationRegulatorActivated,
            "decrease_intensity_regulator_activated" => $this->decreaseIntensityIlluminationRegulatorActivated
        ]);
        $message = new Message($payload);
        $this->client->send("/topic/lecturas_iluminacion_oficina1", $message);
        if ($this->illuminationIntensity >= self::INCREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY) {
            $this->increaseIntensityIlluminationRegulatorActivated = false;
        }
    }
    
    public function manageDecreaseIlluminationRegulator() {
        if (!$this->decreaseIntensityIlluminationRegulatorActivated) {
            $this->decreaseIntensityIlluminationRegulatorActivated = true;
        }
        $this->illuminationIntensity -= self::ILLUMINATION_INTENSITY_DIFFERENCE;
        $payload = json_encode([
            "office" => "office1",
            "illumination_intensity" => $this->illuminationIntensity,
            "increase_intensity_regulator_activated" => $this->increaseIntensityIlluminationRegulatorActivated,
            "decrease_intensity_regulator_activated" => $this->decreaseIntensityIlluminationRegulatorActivated
        ]);
        $message = new Message($payload);
        $this->client->send("/topic/lecturas_iluminacion_oficina1", $message);
        if ($this->illuminationIntensity <= self::DECREASE_INTENSITY_ILLUMINATION_REGULATOR_STOP_INTENSITY) {
            $this->decreaseIntensityIlluminationRegulatorActivated = false;
        }
    }
    
    // Illumination flag management methods
    public function getActivateIncreaseIntensityFlag($text) {
        $json = json_decode($text, true);
        return isset($json["activate_increase_intensity_illumination_regulator"]) ? $json["activate_increase_intensity_illumination_regulator"] : null;
    }
    public function getStopIncreaseIntensityFlag($text) {
        $json = json_decode($text, true);
        return isset($json["stop_increase_intensity_illumination_regulator"]) ? $json["stop_increase_intensity_illumination_regulator"] : null;
    }
    public function getActivateDecreaseIntensityFlag($text) {
        $json = json_decode($text, true);
        return isset($json["activate_decrease_intensity_illumination_regulator"]) ? $json["activate_decrease_intensity_illumination_regulator"] : null;
    }
    public function getStopDecreaseIntensityFlag($text) {
        $json = json_decode($text, true);
        return isset($json["stop_decrease_intensity_illumination_regulator"]) ? $json["stop_decrease_intensity_illumination_regulator"] : null;
    }
    public function manageIlluminationFlags($text) {
        $activateIncrease = $this->getActivateIncreaseIntensityFlag($text);
        $stopIncrease = $this->getStopIncreaseIntensityFlag($text);
        $activateDecrease = $this->getActivateDecreaseIntensityFlag($text);
        $stopDecrease = $this->getStopDecreaseIntensityFlag($text);
        
        if ($activateIncrease !== null && $activateIncrease) {
            $this->increaseIntensityIlluminationRegulatorActivated = true;
        }
        if ($stopIncrease !== null && $stopIncrease) {
            $this->increaseIntensityIlluminationRegulatorActivated = false;
        }
        if ($activateDecrease !== null && $activateDecrease) {
            $this->decreaseIntensityIlluminationRegulatorActivated = true;
        }
        if ($stopDecrease !== null && $stopDecrease) {
            $this->decreaseIntensityIlluminationRegulatorActivated = false;
        }
    }
    public function printOwnIlluminationInformation() {
        echo "Illumination Sensor: " . $this->illuminationIntensity . " lumens\n";
        if ($this->increaseIntensityIlluminationRegulatorActivated) {
            echo "Illumination Activator: Increase Intensity Regulator activated.\n";
        }
        if ($this->decreaseIntensityIlluminationRegulatorActivated) {
            echo "Illumination Activator: Decrease Intensity Regulator activated.\n";
        }
    }
    public function printReceivedIlluminationInformation($text) {
        if ($this->getActivateIncreaseIntensityFlag($text) !== null && $this->getActivateIncreaseIntensityFlag($text)) {
            echo "Illumination Activator: Illumination intensity is below threshold (below 1500 lumens). Activating Increase Intensity Regulator...\n";
        }
        if ($this->getStopIncreaseIntensityFlag($text) !== null && $this->getStopIncreaseIntensityFlag($text)) {
            echo "Illumination Activator: Illumination intensity has reached desired level (around 2300 lumens). Stopping Increase Intensity Regulator...\n";
        }
        if ($this->getActivateDecreaseIntensityFlag($text) !== null && $this->getActivateDecreaseIntensityFlag($text)) {
            echo "Illumination Activator: Illumination intensity is above threshold (above 3000 lumens). Activating Decrease Intensity Regulator...\n";
        }
        if ($this->getStopDecreaseIntensityFlag($text) !== null && $this->getStopDecreaseIntensityFlag($text)) {
            echo "Illumination Activator: Illumination intensity has reached desired level (around 2300 lumens). Stopping Decrease Intensity Regulator...\n";
        }
    }
    
    // Simulated onMessage method. The callback from subscriptions will pass a frame object with a 'body' property.
    public function onMessage($frame) {
        $body = trim($frame->body);
        if ($body !== "") {
            if (strpos($body, '"temperature"') !== false) {
                $this->manageTemperatureFlags($body);
                $this->printReceivedTemperatureInformation($body);
            }
            if (strpos($body, '"illumination_intensity"') !== false) {
                $this->manageIlluminationFlags($body);
                $this->printReceivedIlluminationInformation($body);
            }
        } else {
            echo "Received empty message.\n";
        }
    }
    
    public function printReceivedTemperatureInformation($text) {
        if ($this->getActivateColdSystemFlag($text) !== null && $this->getActivateColdSystemFlag($text)) {
            echo "Temperature Activator: Temperature has exceeded 30C. Activating Cold System...\n";
        }
        if ($this->getStopColdSystemFlag($text) !== null && $this->getStopColdSystemFlag($text)) {
            echo "Temperature Activator: Temperature has reached 23C or less. Stopping Cold System...\n";
        }
        if ($this->getActivateHeatSystemFlag($text) !== null && $this->getActivateHeatSystemFlag($text)) {
            echo "Temperature Activator: Temperature is below 15C. Activating Heat System...\n";
        }
        if ($this->getStopHeatSystemFlag($text) !== null && $this->getStopHeatSystemFlag($text)) {
            echo "Temperature Activator: Temperature has reached 23C or more. Stopping Heat System...\n";
        }
    }
    
    // Starts the connection, subscriptions, and periodic tasks.
    public function start() {
        echo "Office1 Starting...\n";
        // Subscribe to actuador topics
        $this->client->subscribe("/topic/actuador_temperatura_oficina1", function($frame) {
            $this->onMessage($frame);
        });
        $this->client->subscribe("/topic/actuador_iluminacion_oficina1", function($frame) {
            $this->onMessage($frame);
        });
        // Schedule periodic temperature task every 2 seconds
        $this->loop->addPeriodicTimer(2, function() {
            if (!$this->coldSystemActivated && !$this->heatSystemActivated) {
                $this->setTemperature(Utils::manejarTemperaturaRandomIndicator());
            }
            if ($this->heatSystemActivated) {
                $this->manageHeatSystem();
            } elseif ($this->coldSystemActivated) {
                $this->manageColdSystem();
            } else {
                $this->sendTemperatureMessage();
            }
            $this->printOwnTemperatureInformation();
        });
        // Schedule periodic illumination task every 2 seconds
        $this->loop->addPeriodicTimer(2, function() {
            if (!$this->increaseIntensityIlluminationRegulatorActivated && !$this->decreaseIntensityIlluminationRegulatorActivated) {
                $this->setIlluminationIntensity(Utils::manejarIlluminationIntensityRandomIndicator());
            }
            if ($this->increaseIntensityIlluminationRegulatorActivated) {
                $this->manageIncreaseIlluminationRegulator();
            } elseif ($this->decreaseIntensityIlluminationRegulatorActivated) {
                $this->manageDecreaseIlluminationRegulator();
            } else {
                $this->sendIlluminationMessage();
            }
            $this->printOwnIlluminationInformation();
        });
    }
}

// Main execution
$loop = Factory::create();
$client = new Client("tcp://localhost:61613");
$client->setLogin("mtis", "mtis");

$oficina1 = new Oficina1($client, $loop);
$oficina1->start();
$loop->run();
