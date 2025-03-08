// Oficina2.js
// This file replicates the functionality of Oficina1.java in Node.js
// It communicates with ActiveMQ via STOMP and uses the topics for Office2.
// To run: node Oficina2.js
// Make sure to install the dependency: npm install stomp-client

var StompClient = require('stomp-client');

// Broker settings
var BROKER_HOST = 'localhost';
var BROKER_PORT = 61613; // STOMP port
var USER = 'mtis';
var PASS = 'mtis';

// Topics for Office2
var LECTURAS_TEMP_TOPIC = '/topic/lecturas_temperaturas_oficina2';
var ACTUADOR_TEMP_TOPIC = '/topic/actuador_temperatura_oficina2';
var LECTURAS_ILLUM_TOPIC = '/topic/lecturas_iluminacion_oficina2';
var ACTUADOR_ILLUM_TOPIC = '/topic/actuador_iluminacion_oficina2';

// Internal state variables
var temperature = 0;
var heatSystemActivated = false;
var coldSystemActivated = false;

var illuminationIntensity = 2300; // equilibrium value
var increaseIntensityRegulatorActivated = false;
var decreaseIntensityRegulatorActivated = false;

// Counters to simulate random indicators (to mimic the Java Utils methods)
var temperaturaRandomIndicator = 0;
var illuminationRandomIndicator = 0;

// Functions to generate pseudo-random values similar to Utils.java
function manejarTemperaturaRandomIndicator() {
    var modulo = temperaturaRandomIndicator % 8;
    var randomNumber = 0;
    if (modulo === 0) {
        randomNumber = Math.floor(Math.random() * 6) + 25;
    } else if (modulo === 1) {
        randomNumber = Math.floor(Math.random() * 6) + 20;
    } else if (modulo === 2) {
        randomNumber = Math.floor(Math.random() * 6) + 15;
    } else if (modulo === 3) {
        randomNumber = Math.floor(Math.random() * 15);
    } else if (modulo === 4) {
        randomNumber = Math.floor(Math.random() * 6) + 15;
    } else if (modulo === 5) {
        randomNumber = Math.floor(Math.random() * 6) + 20;
    } else if (modulo === 6) {
        randomNumber = Math.floor(Math.random() * 6) + 25;
    } else if (modulo === 7) {
        randomNumber = Math.floor(Math.random() * 15) + 31;
    }
    temperaturaRandomIndicator++;
    return randomNumber;
}

function manejarIlluminationIntensityRandomIndicator() {
    var modulo = illuminationRandomIndicator % 12;
    var randomNumber = 0;
    if (modulo === 0) {
        randomNumber = Math.floor(Math.random() * 301) + 1500;
    } else if (modulo === 1) {
        randomNumber = Math.floor(Math.random() * 301) + 1800;
    } else if (modulo === 2) {
        randomNumber = Math.floor(Math.random() * 301) + 2100;
    } else if (modulo === 3) {
        randomNumber = Math.floor(Math.random() * 301) + 2400;
    } else if (modulo === 4) {
        randomNumber = Math.floor(Math.random() * 301) + 2700;
    } else if (modulo === 5) {
        randomNumber = Math.floor(Math.random() * 1500) + 3001;
    } else if (modulo === 6) {
        randomNumber = Math.floor(Math.random() * 301) + 2700;
    } else if (modulo === 7) {
        randomNumber = Math.floor(Math.random() * 301) + 2400;
    } else if (modulo === 8) {
        randomNumber = Math.floor(Math.random() * 301) + 2100;
    } else if (modulo === 9) {
        randomNumber = Math.floor(Math.random() * 301) + 1800;
    } else if (modulo === 10) {
        randomNumber = Math.floor(Math.random() * 301) + 1500;
    } else if (modulo === 11) {
        randomNumber = Math.floor(Math.random() * 1500);
    }
    illuminationRandomIndicator++;
    return randomNumber;
}

// Create a STOMP client
var client = new StompClient(BROKER_HOST, BROKER_PORT, USER, PASS);

// Connect to the broker
client.connect(function(sessionId) {
    console.log("Connected with sessionId:", sessionId);
    
    // Subscribe to actuator topics for Office2
    client.subscribe(ACTUADOR_TEMP_TOPIC, function(body, headers) {
        // Parse JSON message
        try {
            var data = JSON.parse(body);
            // Process temperature flags and print received info (for Office2)
            printReceivedTemperatureInformation(data);
        } catch (e) {
            console.log("Error parsing JSON in temperature actuator message:", e);
        }
    });

    client.subscribe(ACTUADOR_ILLUM_TOPIC, function(body, headers) {
        // Parse JSON message
        try {
            var data = JSON.parse(body);
            // Process illumination flags and print received info (for Office2)
            printReceivedIlluminationInformation(data);
        } catch (e) {
            console.log("Error parsing JSON in illumination actuator message:", e);
        }
    });
    
    // Periodic tasks for temperature and illumination (every 2 seconds)
    setInterval(function() {
        if (!coldSystemActivated && !heatSystemActivated) {
            temperature = manejarTemperaturaRandomIndicator();
        }
        if (heatSystemActivated) {
            manageHeatSystem();
        } else if (coldSystemActivated) {
            manageColdSystem();
        } else {
            sendTemperatureMessage();
        }
        console.log("Office2 Temperature Sensor:", temperature, "C");
    }, 2000);

    setInterval(function() {
        if (!increaseIntensityRegulatorActivated && !decreaseIntensityRegulatorActivated) {
            illuminationIntensity = manejarIlluminationIntensityRandomIndicator();
        }
        if (increaseIntensityRegulatorActivated) {
            manageIncreaseIlluminationRegulator();
        } else if (decreaseIntensityRegulatorActivated) {
            manageDecreaseIlluminationRegulator();
        } else {
            sendIlluminationMessage();
        }
        console.log("Office2 Illumination Sensor:", illuminationIntensity, "lumens");
    }, 2000);
});

// Function to send temperature reading message
function sendTemperatureMessage() {
    var message = {
        office: "office2",
        temperature: temperature,
        cold_system_activated: coldSystemActivated,
        heat_system_activated: heatSystemActivated
    };
    client.publish(LECTURAS_TEMP_TOPIC, JSON.stringify(message));
}

// Function to send illumination reading message
function sendIlluminationMessage() {
    var message = {
        office: "office2",
        illumination_intensity: illuminationIntensity,
        increase_intensity_regulator_activated: increaseIntensityRegulatorActivated,
        decrease_intensity_regulator_activated: decreaseIntensityRegulatorActivated
    };
    client.publish(LECTURAS_ILLUM_TOPIC, JSON.stringify(message));
}

// Functions to manage temperature systems
function manageHeatSystem() {
    if (!heatSystemActivated) {
        heatSystemActivated = true;
    }
    temperature += 2; // Increment temperature by difference
    sendTemperatureMessage();
    if (temperature >= 23) {
        heatSystemActivated = false;
    }
}

function manageColdSystem() {
    if (!coldSystemActivated) {
        coldSystemActivated = true;
    }
    temperature -= 2; // Decrement temperature by difference
    sendTemperatureMessage();
    if (temperature <= 23) {
        coldSystemActivated = false;
    }
}

// Functions to manage illumination regulators
function manageIncreaseIlluminationRegulator() {
    if (!increaseIntensityRegulatorActivated) {
        increaseIntensityRegulatorActivated = true;
    }
    illuminationIntensity += 200;
    sendIlluminationMessage();
    if (illuminationIntensity >= 2300) {
        increaseIntensityRegulatorActivated = false;
    }
}

function manageDecreaseIlluminationRegulator() {
    if (!decreaseIntensityRegulatorActivated) {
        decreaseIntensityRegulatorActivated = true;
    }
    illuminationIntensity -= 200;
    sendIlluminationMessage();
    if (illuminationIntensity <= 2300) {
        decreaseIntensityRegulatorActivated = false;
    }
}

// Functions to print received messages (simulate actuator instructions)
// For temperature (exactly 6 if statements)
function printReceivedTemperatureInformation(data) {
    var temp = data.temperature;
    if (temp > 30 && data.cold_system_activated === false) {
        console.log("Temperature Activator (Office2): Temperature has exceeded 30C. Activating Cold System...");
    }
    if (temp > 23 && data.cold_system_activated === true) {
        console.log("Temperature Activator (Office2): Cold System is activated.");
    }
    if (temp <= 23 && data.cold_system_activated === true) {
        console.log("Temperature Activator (Office2): Temperature has reached 23C or less. Requesting Office2 to stop Cold System...");
    }
    if (temp < 15 && data.heat_system_activated === false) {
        console.log("Temperature Activator (Office2): Temperature is below 15C. Activating Heat System...");
    }
    if (temp < 23 && data.heat_system_activated === true) {
        console.log("Temperature Activator (Office2): Heat System is activated.");
    }
    if (temp >= 23 && data.heat_system_activated === true) {
        console.log("Temperature Activator (Office2): Temperature has reached 23C or more. Requesting Office2 to stop Heat System...");
    }
}

// For illumination (exactly 6 if statements)
function printReceivedIlluminationInformation(data) {
    var illum = data.illumination_intensity;
    if (illum < 1500 && data.increase_intensity_regulator_activated === false) {
        console.log("Illumination Activator (Office2): Illumination is below threshold (below 1500 lumens). Activating Increase Intensity Regulator...");
    }
    if (illum < 2300 && data.increase_intensity_regulator_activated === true) {
        console.log("Illumination Activator (Office2): Increase Intensity Regulator is activated.");
    }
    if (illum >= 2300 && data.increase_intensity_regulator_activated === true) {
        console.log("Illumination Activator (Office2): Illumination has reached desired level (around 2300 lumens). Requesting Office2 to stop Increase Intensity Regulator...");
    }
    if (illum > 3000 && data.decrease_intensity_regulator_activated === false) {
        console.log("Illumination Activator (Office2): Illumination is above threshold (above 3000 lumens). Activating Decrease Intensity Regulator...");
    }
    if (illum > 2300 && data.decrease_intensity_regulator_activated === true) {
        console.log("Illumination Activator (Office2): Decrease Intensity Regulator is activated.");
    }
    if (illum <= 2300 && data.decrease_intensity_regulator_activated === true) {
        console.log("Illumination Activator (Office2): Illumination has reached desired level (around 2300 lumens). Requesting Office2 to stop Decrease Intensity Regulator...");
    }
}
