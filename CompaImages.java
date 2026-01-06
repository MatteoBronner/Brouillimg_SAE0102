import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import java.io.File;

import java.io.IOException;

public class CompaImages {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {

            System.err.println(
                    "Usage: java CompaImages <image_claire> <image_décrypté>");

            System.exit(1);

        } 

        String inPath1 = args[0];
        String inPath2 = args[1];

        BufferedImage inputImage1 = ImageIO.read(new File(inPath1));
        BufferedImage inputImage2 = ImageIO.read(new File(inPath2));

        int resemblance = ImageIdentique(inputImage1, inputImage2);
        System.out.println("différence entre les images: ~" + (resemblance * 100)/(inputImage1.getWidth() * inputImage1.getHeight()) + "% (" + resemblance + " pixels différents).");
    }

    /**
     * méthode qui permet de verifié si deux image son les méme
     * nécéssaire pour les test
     * @param img1 image d'origine
     * @param img2 une deuxieme image
     * @return le pourcentage de resemblance des images
     * */
    public static int ImageIdentique(BufferedImage img1, BufferedImage img2) {

        if (img1.getWidth() != img2.getWidth() ||
                img1.getHeight() != img2.getHeight()) {
            return 100;
        }

        int dif = 0;

        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    dif += 1;
                }
            }
        }
        
        return dif;
    }
}