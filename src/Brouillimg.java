import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import java.io.File;

import java.io.IOException;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.atomic.AtomicLong;

import java.util.stream.IntStream;


public class Brouillimg {


    public static void main(String[] args) throws IOException {

        if (args.length < 3) {

            System.err.println("Usage: java Brouillimg <image_claire> <clé> <scramble (0), unscramble(1), break>, [image_sortie]");

            System.exit(1);

        }

        String inPath = args[0];

        String outPath = (args.length >= 4) ? args[3] : "out.png";

        // Masque 0x7FFF pour garantir que la clé ne dépasse pas les 15 bits

        int key = Integer.parseInt(args[1]) & 0x7FFF;
        int choix = Integer.parseInt(args[2]);


        BufferedImage inputImage = ImageIO.read(new File(inPath));

        if (inputImage == null) {

            throw new IOException("Format d’image non reconnu: " + inPath);

        }


        final int height = inputImage.getHeight();

        final int width = inputImage.getWidth();

        System.out.println("Dimensions de l'image : " + width + "x" + height);


        // Pré‑calcul des lignes en niveaux de gris pour accélérer le calcul du critère

        int[][] inputImageGL = rgb2gl(inputImage);


        int[] perm = generatePermutation(height, key);


        if (choix == 0) {
            System.out.println("cryptage");

            BufferedImage scrambledImage = scrambleLines(inputImage, perm);

            ImageIO.write(scrambledImage, "png", new File(outPath));

        }else if(choix == 1) {

            System.out.println("decryptage");
            BufferedImage unscrambledImage = unscrambleLines(inputImage, perm);

            ImageIO.write(unscrambledImage, "png", new File(outPath));
        }else{
            System.out.println("decryptage brute force");
            BufferedImage breakKey = breakKey(inputImage);

            ImageIO.write(breakKey, "png", new File(outPath));

        }

        System.out.println("Image écrite: " + outPath);

    }


    /**
     * Convertit une image RGB en niveaux de gris (GL).
     *
     * @param inputRGB image d'entrée en RGB
     * @return tableau 2D des niveaux de gris (0-255)
     */

    public static int[][] rgb2gl(BufferedImage inputRGB) {

        final int height = inputRGB.getHeight();

        final int width = inputRGB.getWidth();

        int[][] outGL = new int[height][width];


        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                int argb = inputRGB.getRGB(x, y);

                int r = (argb >> 16) & 0xFF;

                int g = (argb >> 8) & 0xFF;

                int b = argb & 0xFF;

                // luminance simple (évite float)

                int gray = (r * 299 + g * 587 + b * 114) / 1000;

                outGL[y][x] = gray;

            }

        }

        return outGL;

    }


    /**
     * Génère une permutation des entiers 0..size-1 en fonction d'une clé.
     *
     * @param size taille de la permutation
     * @param key  clé de génération (15 bits)
     * @return tableau de taille 'size' contenant une permutation des entiers 0..size-1
     */

    public static int[] generatePermutation(int size, int key) {

        int[] scrambleTable = new int[size];

        for (int i = 0; i < size; i++) scrambleTable[i] = scrambledId(i, size, key);

        return scrambleTable;

    }


    /**
     * Mélange les lignes d'une image selon une permutation donnée.
     *
     * @param inputImg image d'entrée
     * @param perm     permutation des lignes (taille = hauteur de l'image)
     * @return image de sortie avec les lignes mélangées
     */

    public static BufferedImage scrambleLines(BufferedImage inputImg, int[] perm) {

        int width = inputImg.getWidth();

        int height = inputImg.getHeight();

        if (perm.length != height) throw new IllegalArgumentException("Taille d'image <> taille permutation");


        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {

            int newY = perm[y];

            for (int x = 0; x < width; x++) {

                out.setRGB(x, newY, inputImg.getRGB(x, y));
            }
        }
        return out;

    }


    public static BufferedImage unscrambleLines(BufferedImage inputImg, int[] perm) {

        int width = inputImg.getWidth();

        int height = inputImg.getHeight();

        if (perm.length != height) throw new IllegalArgumentException("Taille d'image <> taille permutation");


        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {

            int newY = perm[y];

            for (int x = 0; x < width; x++) {

                out.setRGB(x, y, inputImg.getRGB(x, newY));
            }
        }
        return out;

    }


    /**
     * Renvoie la position de la ligne id dans l'image brouillée.
     *
     * @param id   indice de la ligne dans l'image claire (0..size-1)
     * @param size nombre total de lignes dans l'image
     * @param key  clé de brouillage (15 bits)
     * @return indice de la ligne dans l'image brouillée (0..size-1)
     */

    public static int scrambledId(int id, int size, int key) {
        int s = key & 0x7F;
        int r = (key >> 6) & 0xFF;

        id = (r + (2 * s + 1) * id) % size;
        return id;
    }

    /**
     * Calcule la distance euclidienne entre deux lignes d'une image en niveaux de gris
     *
     * @param line1 premiere
     * @param line2 et deuxieme digne les deux en nivaux de gris
     * @return distance euclidienne
     */
    public static double euclideanDistance(int[] line1, int[] line2) {
        if (line1.length != line2.length) {
            throw new IllegalArgumentException("Lignes de tailles différentes");
        }

        long sum = 0;
        for (int x = 0; x < line1.length; x++) {
            int diff = line1[x] - line2[x];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * Calcule le score euclidien d'une image.
     * @param imageGL l'image en niveaux de gris
     * @return score euclidien de l'image entrée
     */
    public static double scoreEuclidean(int[][] imageGL) {
        int height = imageGL.length;
        double score = 0.0;

        for (int y = 0; y < height - 1; y++) {
            score += euclideanDistance(imageGL[y], imageGL[y + 1]);
        }
        return score;
    }

    /**
     * Teste TOUTE les clés possibles et retourne l'image déchiffrée qui correspondant au meilleur score euclidien.

     * @param scrambledImg image brouillée
     * @return image la plus probable
     */
    public static BufferedImage breakKey(BufferedImage scrambledImg) {

        int height = scrambledImg.getHeight();

        double bestScore = Double.MAX_VALUE;
        int bestKey = -1;
        BufferedImage bestImage = null;

        for (int testkey = 0; testkey < 32768; testkey++) {

            int[] permutationtest = generatePermutation(height, testkey);
            BufferedImage candida = unscrambleLines(scrambledImg, permutationtest);

            int[][] gl = rgb2gl(candida);
            double score = scoreEuclidean(gl);

            if (score < bestScore) {
                bestScore = score;
                bestKey = testkey;
                bestImage = candida;
            }
        }

        System.out.println("Meilleure clé trouvée : " + bestKey);
        System.out.println("Score euclidien : " + bestScore);

        return bestImage;
    }


}