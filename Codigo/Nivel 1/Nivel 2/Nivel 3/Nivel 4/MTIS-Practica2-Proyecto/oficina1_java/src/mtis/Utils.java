package mtis;

import java.util.Random;

public final class Utils {
	
    private static int temperaturaRandomIndicator = 0;

    private Utils() {
        throw new UnsupportedOperationException("ERROR: No se puede instanciar esta clase.");
    }


    
    public static int manejarTemperaturaRandomIndicator() {
        Random random = new Random();
        int indicadorModulo = temperaturaRandomIndicator % 8;
        int randomNumber = 0;
        if (indicadorModulo == 0) {
            randomNumber = random.nextInt(6) + 25;
        } else if (indicadorModulo == 1) {
            randomNumber = random.nextInt(6) + 20;
        } else if (indicadorModulo == 2) {
            randomNumber = random.nextInt(6) + 15;
        } else if (indicadorModulo == 3) {
            randomNumber = random.nextInt(15);
        } else if (indicadorModulo == 4) {
            randomNumber = random.nextInt(6) + 15;
        } else if (indicadorModulo == 5) {
            randomNumber = random.nextInt(6) + 20;
        } else if (indicadorModulo == 6) {
            randomNumber = random.nextInt(6) + 25;
        } else if (indicadorModulo == 7) {
            randomNumber = random.nextInt(6) + 40;
        }
        temperaturaRandomIndicator++;
        return randomNumber;
    }


}