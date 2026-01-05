import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import java.io.File;

import java.io.IOException;

/*import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.atomic.AtomicLong;

import java.util.stream.IntStream;*/

public class Brouillimg {

    public static void main(String[] args) throws IOException {
        /*test breakKey euclidian a metre en commentaire aprés les test*/
        //runImageTest();

        if (args.length < 2) {

            System.err.println(
                    "Usage: java Brouillimg <image_claire> <0 = scramble | 1 = unscramble | 2 = decryptage (distance euclidienne) |3 = decyptage (correlation de Pearson)> <clé> [image_sortie]");

            System.exit(1);

        }

        String inPath = args[0];

        String outPath = (args.length >= 4) ? args[3] : "out.png";

        // Masque 0x7FFF pour garantir que la clé ne dépasse pas les 15 bits
        int key = 0;
        if (args.length != 2) {
            key = Integer.parseInt(args[2]) & 0x7FFF;
        }

        int choix = Integer.parseInt(args[1]);

        BufferedImage inputImage = ImageIO.read(new File(inPath));

        if (inputImage == null) {

            throw new IOException("Format d’image non reconnu: " + inPath);

        }

        final int height = inputImage.getHeight();

        final int width = inputImage.getWidth();

        System.out.println("Dimensions de l'image : " + width + "x" + height);

        // Pré‑calcul des lignes en niveaux de gris pour accélérer le calcul du critère

        // int[][] inputImageGL = rgb2gl(inputImage);

        int[] perm = generatePermutation(height, key);


        switch (choix) {
            case 0:
                System.out.println("cryptage");

                BufferedImage scrambledImage = scrambleLines(inputImage, perm);
                ImageIO.write(scrambledImage, "png", new File(outPath));
                break;

            case 1:
                System.out.println("decryptage");

                BufferedImage unscrambledImage = unscrambleLines(inputImage, perm);
                ImageIO.write(unscrambledImage, "png", new File(outPath));
                break;

            case 2:
                Profiler.init();

                System.out.println("decryptage (distance euclidienne)");

                final BufferedImage[] result = new BufferedImage[1];

                Profiler.analyse(() -> {
                    result[0] = breakKey(inputImage, 0);
                });

                ImageIO.write(result[0], "png", new File(outPath));

                Profiler.getGlobalTime();
                break;


            case 3:
                Profiler.init();

                System.out.println("decryptage (correlation de Pearson)");

                final BufferedImage[] resultPe = new BufferedImage[1];

                Profiler.analyse(() -> {
                    resultPe[0] = breakKey(inputImage, 1);
                });

                ImageIO.write(resultPe[0], "png", new File(outPath));

                Profiler.getGlobalTime();
                break;

            default:
                System.err.println(
                        "Usage: java Brouillimg <image_claire> <0 = scramble | 1 = unscramble | 2 = decryptage (distance euclidienne) |3 = decyptage (correlation de Pearson)> <clé> [image_sortie]");

                System.exit(1);
                return;
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
     * @return tableau de taille 'size' contenant une permutation des entiers
     *         0..size-1
     */

    public static int[] generatePermutation(int size, int key) {

        int[] scrambleTable = new int[size];

        for (int i = 0; i < size; i++)
            scrambleTable[i] = scrambledId(i, size, key);

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

        if (perm.length != height)
            throw new IllegalArgumentException("Taille d'image <> taille permutation");

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

        if (perm.length != height)
            throw new IllegalArgumentException("Taille d'image <> taille permutation");

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {

            int newY = perm[y];

            for (int x = 0; x < width; x++) {

                out.setRGB(x, y, inputImg.getRGB(x, newY));
            }
        }
        return out;

    }

    public static BufferedImage unscrambleLinesPearson(
            BufferedImage inputImg, int[] perm) {

        int width = inputImg.getWidth();
        int height = inputImg.getHeight();

        BufferedImage out = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);

        int[] inv = new int[height];
        for (int i = 0; i < height; i++) {
            inv[perm[i]] = i;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out.setRGB(x, y, inputImg.getRGB(x, inv[y]));
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
     * Calcule la distance euclidienne entre deux lignes d'une image en niveaux de
     * gris
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
     *
     * @param imageGL l'image en niveaux de gris
     * @return score euclidien de l'image entrée
     */
    public static double scoreEuclidean(int[][] imageGL, double bestScore) {
        int height = imageGL.length;
        double score = 0.0;

        for (int y = 0; y < height - 1; y++) {
            score += euclideanDistance(imageGL[y], imageGL[y + 1]);
            if (score > bestScore) {
                return Double.MAX_VALUE;
            }
        }
        return score;
    }

    /**
     * Calcule la correlation de Pearson entre deux lignes d'une image en niveaux de
     * gris
     *
     * @param line1 premiere
     * @param line2 et deuxieme digne les deux en nivaux de gris
     * @return score Pearson
     */
    public static double pearsonCorrelation(int[] line1, int[] line2) {
        if (line1.length != line2.length) {
            throw new IllegalArgumentException("Lignes de tailles différentes");
        }

        int len = line1.length;

        double moyX = moyLine(line1);
        double moyY = moyLine(line2);

        double sumXX = 0;
        double sumYY = 0;
        double sumXY = 0;

        for (int i = 0; i < len; i += 4) {
            double difX = line1[i] - moyX;
            double difY = line2[i] - moyY;

            sumXY += difX * difY;
            sumXX += difX * difX;
            sumYY += difY * difY;
        }

        if (sumXX == 0 || sumYY == 0) {
            return 0.0;
        }

        return sumXY / Math.sqrt(sumXX * sumYY);
    }

    public static double moyLine(int[] line) {
        double sum = 0.0;

        for (int i = 0; i < line.length; ++i) {
            sum += line[i];
        }

        return sum / line.length;
    }

    /**
     * Calcule le score Pearson d'une image.
     *
     * @param imageGL l'image en niveaux de gris
     * @return score Pearson de l'image entrée
     */
    public static double scorePearson(int[][] imageGL, double bestScore) {
        int height = imageGL.length;
        double score = 0.0;

        for (int y = 0; y < height - 1; y++) {
            score += pearsonCorrelation(imageGL[y], imageGL[y + 1]);

            if (score + (height - y - 1) <= bestScore) {
                return Double.NEGATIVE_INFINITY;
            }

        }

        return score;
    }

    /**
     * Applique une permutation inverse aux lignes d'une image en niveaux de gris
     */
    public static int[][] unscrambleLinesGL(int[][] inputGL, int[] perm) {

        int height = inputGL.length;
        int[][] outGL = new int[height][];

        for (int y = 0; y < height; y++) {
            outGL[y] = inputGL[perm[y]];
        }
        return outGL;
    }

    public static int[][] unscrambleLinesGLPearson(int[][] inputGL, int[] perm) {

        int height = inputGL.length;
        int[][] outGL = new int[height][];

        int[] inv = new int[height];
        for (int i = 0; i < height; i++) {
            inv[perm[i]] = i;
        }

        for (int y = 0; y < height; y++) {
            outGL[y] = inputGL[inv[y]];
        }
        return outGL;
    }

    /**
     * Teste TOUTE les clés possibles et retourne l'image déchiffrée qui
     * correspondant au meilleur score euclidien.
     *
     * @param inputImage image brouillée
     * @return image la plus probable
     */
    public static BufferedImage breakKey(BufferedImage inputImage, int choix) {

        int height = inputImage.getHeight();

        int[][] ImageGL = rgb2gl(inputImage);

        double bestScore;
        if (choix == 0) {
            bestScore = Double.MAX_VALUE;
        } else {
            bestScore = -Double.MAX_VALUE;
        }

        int bestKey = -1;

        for (int testkey = 0; testkey < 32768; testkey++) {

            int[] permutationtest = generatePermutation(height, testkey);

            if (choix == 0) {
                int[][] candidaGl = unscrambleLinesGL(ImageGL, permutationtest);
                double score = scoreEuclidean(candidaGl, bestScore);

                if (score < bestScore) {
                    bestScore = score;
                    bestKey = testkey;
                }

            } else {
                int[][] candidaGl = unscrambleLinesGLPearson(ImageGL, permutationtest);
                double score = scorePearson(candidaGl, bestScore);

                if (score > bestScore) {
                    bestScore = score;
                    bestKey = testkey;

                }
            }
        }

        System.out.println("Meilleure clé trouvée : " + bestKey);

        if (choix == 0) {
            System.out.println("Score euclidien : " + bestScore);
        } else {
            System.out.println("Score pearson : " + bestScore);
        }

        int[] LaBonnePermutation = generatePermutation(height, bestKey);

        BufferedImage PBestImg;
        if (choix == 0) {
            PBestImg = unscrambleLines(inputImage, LaBonnePermutation);
        } else {
            PBestImg = unscrambleLinesPearson(inputImage, LaBonnePermutation);
        }

        return PBestImg;
    }
    /**
     * méthode qui permet de verifié si deux image son les méme
     * nécéssaire pour les test
     * @param img1 image d'origine
     * @param img2 une deuxieme image
     * @return true or false si les image corresponde ou pas
     * */
    public static boolean IageIdentique(BufferedImage img1, BufferedImage img2) {

        if (img1.getWidth() != img2.getWidth() ||
                img1.getHeight() != img2.getHeight()) {
            return false;
        }

        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
    /** methode de qui fait les test
     * @return pas mal si les image corresponde pas bon sinon
     * */
    public static void runImageTest() throws IOException {


        String[] inputImages = {
                "image/imageClair/img.png",
                "image/imageClair/img2.png",
                "image/imageClair/img3.png"
        };
        String[] outputImages = {
                "image/imageBroulliée/img2_921.png",
                "image/imageBroulliée/img2_1231.png",
                "image/imageBroulliée/img_488.png"
        };

        int[] secretKeys = {5678, 348, 7638};

        for (int i = 0; i < inputImages.length; i++) {

            System.out.println("Test image : " + inputImages[i]);


            BufferedImage original = ImageIO.read(new File(inputImages[i]));

            if (original == null) {
                throw new IOException("Impossible de lire " + inputImages[i]);
            }


            int[] perm = generatePermutation(original.getHeight(), secretKeys[i]);
            BufferedImage scrambled = scrambleLines(original, perm);


            BufferedImage déchifrée = breakKey(scrambled,0);


            ImageIO.write(déchifrée, "png", new File(outputImages[i]));


            boolean identique = IageIdentique(original, déchifrée);

            System.out.println("Résultat : " + (identique ? "pas mal" : "pas bon"));
            System.out.println("Image écrite : " + outputImages[i]);
        }


        System.out.println("fin du test jarvis OUT");
    }


}