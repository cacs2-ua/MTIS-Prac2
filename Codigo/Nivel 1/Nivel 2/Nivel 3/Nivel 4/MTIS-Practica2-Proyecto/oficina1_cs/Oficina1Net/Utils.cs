using System;

namespace mtis
{
    public static class Utils
    {
        private static int temperaturaRandomIndicator = 0;
        private static int illuminationIntensityRandomIndicator = 0;

        /// <summary>
        /// Genera un valor de temperatura de forma pseudoaleatoria en función de un indicador.
        /// </summary>
        public static int ManejarTemperaturaRandomIndicator()
        {
            // Se crea una nueva instancia de Random (similar a Java)
            Random random = new Random();
            int indicadorModulo = temperaturaRandomIndicator % 8;
            int randomNumber = 0;

            if (indicadorModulo == 0)
            {
                randomNumber = random.Next(6) + 25;
            }
            else if (indicadorModulo == 1)
            {
                randomNumber = random.Next(6) + 20;
            }
            else if (indicadorModulo == 2)
            {
                randomNumber = random.Next(6) + 15;
            }
            else if (indicadorModulo == 3)
            {
                randomNumber = random.Next(15);
            }
            else if (indicadorModulo == 4)
            {
                randomNumber = random.Next(6) + 15;
            }
            else if (indicadorModulo == 5)
            {
                randomNumber = random.Next(6) + 20;
            }
            else if (indicadorModulo == 6)
            {
                randomNumber = random.Next(6) + 25;
            }
            else if (indicadorModulo == 7)
            {
                randomNumber = random.Next(15) + 31;
            }

            temperaturaRandomIndicator++;
            return randomNumber;
        }

        /// <summary>
        /// Genera un valor de intensidad de iluminación de forma pseudoaleatoria en función de un indicador.
        /// </summary>
        public static int ManejarIlluminationIntensityRandomIndicator()
        {
            Random random = new Random();
            int indicadorModulo = illuminationIntensityRandomIndicator % 12;
            int randomNumber = 0;

            if (indicadorModulo == 0)
            {
                randomNumber = random.Next(301) + 1500;
            }
            else if (indicadorModulo == 1)
            {
                randomNumber = random.Next(301) + 1800;
            }
            else if (indicadorModulo == 2)
            {
                randomNumber = random.Next(301) + 2100;
            }
            else if (indicadorModulo == 3)
            {
                randomNumber = random.Next(301) + 2400;
            }
            else if (indicadorModulo == 4)
            {
                randomNumber = random.Next(301) + 2700;
            }
            else if (indicadorModulo == 5)
            {
                randomNumber = random.Next(1500) + 3001;
            }
            else if (indicadorModulo == 6)
            {
                randomNumber = random.Next(301) + 2700;
            }
            else if (indicadorModulo == 7)
            {
                randomNumber = random.Next(301) + 2400;
            }
            else if (indicadorModulo == 8)
            {
                randomNumber = random.Next(301) + 2100;
            }
            else if (indicadorModulo == 9)
            {
                randomNumber = random.Next(301) + 1800;
            }
            else if (indicadorModulo == 10)
            {
                randomNumber = random.Next(301) + 1500;
            }
            else if (indicadorModulo == 11)
            {
                randomNumber = random.Next(1500);
            }

            illuminationIntensityRandomIndicator++;
            return randomNumber;
        }
    }
}
