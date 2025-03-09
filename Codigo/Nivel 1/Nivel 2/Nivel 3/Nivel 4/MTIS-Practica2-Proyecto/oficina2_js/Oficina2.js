// Oficina2.js
// Node.js implementation analogous to Oficina1.java using stompit

const stompit = require('stompit');
const Utils = require('./Utils');

const temperatureDifference = 5;
const heatSystemStopTemperature = 23;
const coldSystemStopTemperature = 23;
const illuminationIntensityDifference = 500;
const increaseIntensityIlluminationRegulatorStopIntensity = 2300;
const decreaseIntensityIlluminationRegulatorStopIntensity = 2300;

class Oficina2 {
    constructor() {
        this.temperature = 0; // initial temperature value
        this.illuminationIntensity = 2300; // initial illumination intensity
        this.heatSystemActivated = false;
        this.coldSystemActivated = false;
        this.increaseIntensityIlluminationRegulatorActivated = false;
        this.decreaseIntensityIlluminationRegulatorActivated = false;
    }
    
    incrementTemperature() {
        this.temperature += temperatureDifference;
    }
    
    decrementTemperature() {
        this.temperature -= temperatureDifference;
    }
    
    sendTemperatureMessage(client) {
        const payload = JSON.stringify({
            office: "office2",
            temperature: this.temperature,
            cold_system_activated: this.coldSystemActivated,
            heat_system_activated: this.heatSystemActivated
        });
        const frame = client.send({destination: '/topic/lecturas_temperaturas_oficina2'});
        frame.write(payload);
        frame.end();
    }
    
    manageColdSystem(client) {
        if (!this.coldSystemActivated) {
            this.coldSystemActivated = true;
        }
        this.decrementTemperature();
        const payload = JSON.stringify({
            office: "office2",
            temperature: this.temperature,
            cold_system_activated: this.coldSystemActivated,
            heat_system_activated: this.heatSystemActivated
        });
        const frame = client.send({destination: '/topic/lecturas_temperaturas_oficina2'});
        frame.write(payload);
        frame.end();
        if (this.temperature <= coldSystemStopTemperature) {
            this.coldSystemActivated = false;
        }
    }
    
    manageHeatSystem(client) {
        if (!this.heatSystemActivated) {
            this.heatSystemActivated = true;
        }
        this.incrementTemperature();
        const payload = JSON.stringify({
            office: "office2",
            temperature: this.temperature,
            cold_system_activated: this.coldSystemActivated,
            heat_system_activated: this.heatSystemActivated
        });
        const frame = client.send({destination: '/topic/lecturas_temperaturas_oficina2'});
        frame.write(payload);
        frame.end();
        if (this.temperature >= heatSystemStopTemperature) {
            this.heatSystemActivated = false;
        }
    }
    
    sendIlluminationMessage(client) {
        const payload = JSON.stringify({
            office: "office2",
            illumination_intensity: this.illuminationIntensity,
            increase_intensity_regulator_activated: this.increaseIntensityIlluminationRegulatorActivated,
            decrease_intensity_regulator_activated: this.decreaseIntensityIlluminationRegulatorActivated
        });
        const frame = client.send({destination: '/topic/lecturas_iluminacion_oficina2'});
        frame.write(payload);
        frame.end();
    }
    
    manageIncreaseIlluminationRegulator(client) {
        if (!this.increaseIntensityIlluminationRegulatorActivated) {
            this.increaseIntensityIlluminationRegulatorActivated = true;
        }
        this.illuminationIntensity += illuminationIntensityDifference;
        const payload = JSON.stringify({
            office: "office2",
            illumination_intensity: this.illuminationIntensity,
            increase_intensity_regulator_activated: this.increaseIntensityIlluminationRegulatorActivated,
            decrease_intensity_regulator_activated: this.decreaseIntensityIlluminationRegulatorActivated
        });
        const frame = client.send({destination: '/topic/lecturas_iluminacion_oficina2'});
        frame.write(payload);
        frame.end();
        if (this.illuminationIntensity >= increaseIntensityIlluminationRegulatorStopIntensity) {
            this.increaseIntensityIlluminationRegulatorActivated = false;
        }
    }
    
    manageDecreaseIlluminationRegulator(client) {
        if (!this.decreaseIntensityIlluminationRegulatorActivated) {
            this.decreaseIntensityIlluminationRegulatorActivated = true;
        }
        this.illuminationIntensity -= illuminationIntensityDifference;
        const payload = JSON.stringify({
            office: "office2",
            illumination_intensity: this.illuminationIntensity,
            increase_intensity_regulator_activated: this.increaseIntensityIlluminationRegulatorActivated,
            decrease_intensity_regulator_activated: this.decreaseIntensityIlluminationRegulatorActivated
        });
        const frame = client.send({destination: '/topic/lecturas_iluminacion_oficina2'});
        frame.write(payload);
        frame.end();
        if (this.illuminationIntensity <= decreaseIntensityIlluminationRegulatorStopIntensity) {
            this.decreaseIntensityIlluminationRegulatorActivated = false;
        }
    }
    
    printOwnTemperatureInformation() {
        console.log("Temperature Sensor: " + this.temperature + "ºC");
        if (this.coldSystemActivated) {
            console.log("Temperature Activator: Cold System activated.");
        }
        if (this.heatSystemActivated) {
            console.log("Temperature Activator: Heat System activated.");
        }
    }
    
    printOwnIlluminationInformation() {
        console.log("Illumination Sensor: " + this.illuminationIntensity + " lumens");
        if (this.increaseIntensityIlluminationRegulatorActivated) {
            console.log("Illumination Activator: Increase Intensity Regulator activated.");
        }
        if (this.decreaseIntensityIlluminationRegulatorActivated) {
            console.log("Illumination Activator: Decrease Intensity Regulator activated.");
        }
    }
    
    processMessage(message) {
        try {
            let data = JSON.parse(message);
            if (data.temperature !== undefined) {
                if (data.activate_cold_system) {
                    this.coldSystemActivated = true;
                    console.log("Temperature Activator: Temperature has exceeded 30ºC. Activating Cold System...");
                }
                if (data.stop_cold_system) {
                    this.coldSystemActivated = false;
                    console.log("Temperature Activator: Temperature has reached 23ºC or less. Stopping Cold System...");
                }
                if (data.activate_heat_system) {
                    this.heatSystemActivated = true;
                    console.log("Temperature Activator: Temperature is below 15ºC. Activating Heat System...");
                }
                if (data.stop_heat_system) {
                    this.heatSystemActivated = false;
                    console.log("Temperature Activator: Temperature has reached 23ºC or more. Stopping Heat System...");
                }
            }
            if (data.illumination_intensity !== undefined) {
                if (data.activate_increase_intensity_illumination_regulator) {
                    this.increaseIntensityIlluminationRegulatorActivated = true;
                    console.log("Illumination Activator: Illumination intensity is below threshold (below 1500 lumens). Activating Increase Intensity Regulator...");
                }
                if (data.stop_increase_intensity_illumination_regulator) {
                    this.increaseIntensityIlluminationRegulatorActivated = false;
                    console.log("Illumination Activator: Illumination intensity has reached desired level (around 2300 lumens). Stopping Increase Intensity Regulator...");
                }
                if (data.activate_decrease_intensity_illumination_regulator) {
                    this.decreaseIntensityIlluminationRegulatorActivated = true;
                    console.log("Illumination Activator: Illumination intensity is above threshold (above 3000 lumens). Activating Decrease Intensity Regulator...");
                }
                if (data.stop_decrease_intensity_illumination_regulator) {
                    this.decreaseIntensityIlluminationRegulatorActivated = false;
                    console.log("Illumination Activator: Illumination intensity has reached desired level (around 2300 lumens). Stopping Decrease Intensity Regulator...");
                }
            }
        } catch (e) {
            console.log("Error processing message: " + e);
        }
    }
    
    start() {
        const connectOptions = {
            host: 'localhost',
            port: 61613,
            connectHeaders: {
                host: '/',
                login: 'mtis',
                passcode: 'mtis'
            }
        };
        stompit.connect(connectOptions, (error, client) => {
            if (error) {
                console.log('Connection error: ' + error.message);
                return;
            }
            console.log("Office1 Starting... ");
            
            // Subscribe to actuador topics for Office2
            client.subscribe({destination: '/topic/actuador_temperatura_oficina2', ack: 'auto'}, (err, message) => {
                if (err) {
                    console.log("Subscribe error (temperature): " + err.message);
                    return;
                }
                message.readString('utf-8', (err, str) => {
                    if (err) {
                        console.log("Read error: " + err.message);
                        return;
                    }
                    this.processMessage(str);
                });
            });
            client.subscribe({destination: '/topic/actuador_iluminacion_oficina2', ack: 'auto'}, (err, message) => {
                if (err) {
                    console.log("Subscribe error (illumination): " + err.message);
                    return;
                }
                message.readString('utf-8', (err, str) => {
                    if (err) {
                        console.log("Read error: " + err.message);
                        return;
                    }
                    this.processMessage(str);
                });
            });
            
            // Temperature task every 5 seconds
            setInterval(() => {
                if (!this.coldSystemActivated && !this.heatSystemActivated) {
                    this.temperature = Utils.manejarTemperaturaRandomIndicator();
                }
                if (this.heatSystemActivated) {
                    this.manageHeatSystem(client);
                } else if (this.coldSystemActivated) {
                    this.manageColdSystem(client);
                } else {
                    this.sendTemperatureMessage(client);
                }
                this.printOwnTemperatureInformation();
            }, 5000);
            
            // Illumination task every 5 seconds
            setInterval(() => {
                if (!this.increaseIntensityIlluminationRegulatorActivated && !this.decreaseIntensityIlluminationRegulatorActivated) {
                    this.illuminationIntensity = Utils.manejarIlluminationIntensityRandomIndicator();
                }
                if (this.increaseIntensityIlluminationRegulatorActivated) {
                    this.manageIncreaseIlluminationRegulator(client);
                } else if (this.decreaseIntensityIlluminationRegulatorActivated) {
                    this.manageDecreaseIlluminationRegulator(client);
                } else {
                    this.sendIlluminationMessage(client);
                }
                this.printOwnIlluminationInformation();
            }, 5000);
        });
    }
}

const office2 = new Oficina2();
office2.start();
