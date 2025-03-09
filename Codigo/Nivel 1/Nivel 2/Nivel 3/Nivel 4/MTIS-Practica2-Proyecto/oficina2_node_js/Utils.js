// Utils.js
// Contains functions equivalent to Utils.java

let temperaturaRandomIndicator = 0;
let illuminationIntensityRandomIndicator = 0;

function manejarTemperaturaRandomIndicator() {
    let indicadorModulo = temperaturaRandomIndicator % 8;
    let randomNumber = 0;
    if (indicadorModulo === 0) {
        randomNumber = Math.floor(Math.random() * 6) + 25;
    } else if (indicadorModulo === 1) {
        randomNumber = Math.floor(Math.random() * 6) + 20;
    } else if (indicadorModulo === 2) {
        randomNumber = Math.floor(Math.random() * 6) + 15;
    } else if (indicadorModulo === 3) {
        randomNumber = Math.floor(Math.random() * 15);
    } else if (indicadorModulo === 4) {
        randomNumber = Math.floor(Math.random() * 6) + 15;
    } else if (indicadorModulo === 5) {
        randomNumber = Math.floor(Math.random() * 6) + 20;
    } else if (indicadorModulo === 6) {
        randomNumber = Math.floor(Math.random() * 6) + 25;
    } else if (indicadorModulo === 7) {
        randomNumber = Math.floor(Math.random() * 15) + 31;
    }
    temperaturaRandomIndicator++;
    return randomNumber;
}

function manejarIlluminationIntensityRandomIndicator() {
    let indicadorModulo = illuminationIntensityRandomIndicator % 12;
    let randomNumber = 0;
    if (indicadorModulo === 0) {
        randomNumber = Math.floor(Math.random() * 301) + 1500;
    } else if (indicadorModulo === 1) {
        randomNumber = Math.floor(Math.random() * 301) + 1800;
    } else if (indicadorModulo === 2) {
        randomNumber = Math.floor(Math.random() * 301) + 2100;
    } else if (indicadorModulo === 3) {
        randomNumber = Math.floor(Math.random() * 301) + 2400;
    } else if (indicadorModulo === 4) {
        randomNumber = Math.floor(Math.random() * 301) + 2700;
    } else if (indicadorModulo === 5) {
        randomNumber = Math.floor(Math.random() * 1500) + 3001;
    } else if (indicadorModulo === 6) {
        randomNumber = Math.floor(Math.random() * 301) + 2700;
    } else if (indicadorModulo === 7) {
        randomNumber = Math.floor(Math.random() * 301) + 2400;
    } else if (indicadorModulo === 8) {
        randomNumber = Math.floor(Math.random() * 301) + 2100;
    } else if (indicadorModulo === 9) {
        randomNumber = Math.floor(Math.random() * 301) + 1800;
    } else if (indicadorModulo === 10) {
        randomNumber = Math.floor(Math.random() * 301) + 1500;
    } else if (indicadorModulo === 11) {
        randomNumber = Math.floor(Math.random() * 1500);
    }
    illuminationIntensityRandomIndicator++;
    return randomNumber;
}

module.exports = {
    manejarTemperaturaRandomIndicator,
    manejarIlluminationIntensityRandomIndicator
};
