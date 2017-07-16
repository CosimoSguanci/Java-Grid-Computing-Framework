
/**
 *
 * @author Cosimo Sguanci
 */
import java.awt.Color;
import java.util.Scanner;
import java.io.*;
import java.awt.image.*;
import javax.imageio.*;

public class Test {

    public static void main(String[] args) throws Exception {
        Grid grid = new Grid();
        grid.init("Project1");
        grid.setTask(new PrimeNumberTask());
        grid.start();

    }
}

class BlurImageTask extends Task {

    public BufferedImage img;
    private File input;
    private String path;

    public BlurImageTask() {

    }

    @Override
    public void setInputData(File input, String path) {
        this.input = input;
        this.path = path;
    }

    @Override
    public void run() {
        readImage();
        blur(10, 49);
        writeImage();

    }

    public void readImage() {
        img = null;
        try {
            img = ImageIO.read(input);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void blur(double sigma, int kernelsize) {
        double[] kernel = createKernel(sigma, kernelsize);
        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                double overflow = 0;
                int counter = 0;
                int kernelhalf = (kernelsize - 1) / 2;
                double red = 0;
                double green = 0;
                double blue = 0;
                for (int k = i - kernelhalf; k < i + kernelhalf; k++) {
                    for (int l = j - kernelhalf; l < j + kernelhalf; l++) {
                        if (k < 0 || k >= img.getWidth() || l < 0 || l >= img.getHeight()) {
                            counter++;
                            overflow += kernel[counter];
                            continue;
                        }

                        Color c = new Color(img.getRGB(k, l));
                        red += c.getRed() * kernel[counter];
                        green += c.getGreen() * kernel[counter];
                        blue += c.getBlue() * kernel[counter];
                        counter++;
                    }
                    counter++;
                }

                if (overflow > 0) {
                    red = 0;
                    green = 0;
                    blue = 0;
                    counter = 0;
                    for (int k = i - kernelhalf; k < i + kernelhalf; k++) {
                        for (int l = j - kernelhalf; l < j + kernelhalf; l++) {
                            if (k < 0 || k >= img.getWidth() || l < 0 || l >= img.getHeight()) {
                                counter++;
                                continue;
                            }

                            Color c = new Color(img.getRGB(k, l));
                            red += c.getRed() * kernel[counter] * (1 / (1 - overflow));
                            green += c.getGreen() * kernel[counter] * (1 / (1 - overflow));
                            blue += c.getBlue() * kernel[counter] * (1 / (1 - overflow));
                            counter++;
                        }
                        counter++;
                    }
                }

                img.setRGB(i, j, new Color((int) red, (int) green, (int) blue).getRGB());
            }
        }
    }

    public double[] createKernel(double sigma, int kernelsize) {
        double[] kernel = new double[kernelsize * kernelsize];
        for (int i = 0; i < kernelsize; i++) {
            double x = i - (kernelsize - 1) / 2;
            for (int j = 0; j < kernelsize; j++) {
                double y = j - (kernelsize - 1) / 2;
                kernel[j + i * kernelsize] = 1 / (2 * Math.PI * sigma * sigma) * Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
            }
        }
        float sum = 0;
        for (int i = 0; i < kernelsize; i++) {
            for (int j = 0; j < kernelsize; j++) {
                sum += kernel[j + i * kernelsize];
            }
        }
        for (int i = 0; i < kernelsize; i++) {
            for (int j = 0; j < kernelsize; j++) {
                kernel[j + i * kernelsize] /= sum;
            }
        }
        return kernel;
    }

    public void lineareAbbildung(double a, double b) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgb = (int) (a * img.getRGB(x, y) + b);
                img.setRGB(x, y, rgb);
            }
        }
    }

    public void blackAndWhite() {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgb = img.getRGB(x, y);
                Color c = new Color(rgb);
                int grey = (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                Color c2 = new Color(grey, grey, grey);
                img.setRGB(x, y, c2.getRGB());
            }
        }
    }

    public void improveGrey() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgb = img.getRGB(x, y);
                if (rgb > max) {
                    max = rgb;
                }
                if (rgb < min) {
                    min = rgb;
                }
            }
        }
        lineareAbbildung(255 / (max - min), - 255 * min / (max - min));
    }

    public void writeImage() {
        File output = new File(path + "result.jpg");
        try {
            ImageIO.write(img, "jpg", output);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

class PrimeNumberTask extends Task {

    private File inputFile;
    private String path;
    private int s1, s2;
    int i;
    int j;
    int flag;
    int x;

    public PrimeNumberTask() {

    }

    @Override
    public void setInputData(File input, String path) {
        this.inputFile = input;
        this.path = path;
    }

    @Override
    public void run() {
        try {
            i = 0;
            flag = 0;
            x = 0;
            j = 0;
            Scanner scanner = new Scanner(inputFile);
            PrintWriter writer = new PrintWriter(path + "result.txt");

            while (scanner.hasNextInt()) {
                if (x == 0) {
                    s1 = scanner.nextInt();
                } else {
                    s2 = scanner.nextInt();
                }
                x++;
            }

            for (i = s1; i <= s2; i++) {
                for (j = 2; j < i; j++) {
                    if (i % j == 0) {
                        flag = 0;
                        break;
                    } else {
                        flag = 1;
                    }
                }
                if (flag == 1) {
                    writer.println(i);
                }
            }

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
