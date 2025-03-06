package mtis;

import java.util.Random;

public final class Utils {
	
    private static int temperaturaRandomIndicator = 0;

    private Utils() {
        throw new UnsupportedOperationException("ERROR: No se puede instanciar esta clase.");
    }


    
    public static int manejarTemperaturaRandomIndicator() {
        Random random = new Random();
        int indicadorModulo = temperaturaRandomIndicator % 3;
        int randomNumber;
        if (indicadorModulo == 0) {
            randomNumber = random.nextInt(16) + 15;
        } else if (indicadorModulo == 1) {
            randomNumber = random.nextInt(15);
        } else {
            randomNumber = random.nextInt(15) + 31;
        }
        temperaturaRandomIndicator++;
        return randomNumber;
    }


}