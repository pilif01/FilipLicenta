package org.example;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

// nu am nevoie de asta

public class ImageHighlighter {

    public static void main(String[] args) {
        String coordinatesFile = "coordinates.txt";  // Path to your file containing filenames and coordinates

        try (BufferedReader br = new BufferedReader(new FileReader(coordinatesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                System.out.println("Trying to access: " + parts[0]);

                String imgPath = "pics/" + parts[0];  // Assuming the images are in a 'pics' subfolder
                int xleft = Integer.parseInt(parts[1]);
                int ytop = Integer.parseInt(parts[2]);
                int xright = Integer.parseInt(parts[3]);
                int ybottom = Integer.parseInt(parts[4]);

                highlightImage(imgPath, xleft, ytop, xright, ybottom);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void highlightImage(String imgPath, int xleft, int ytop, int xright, int ybottom) {
        try {
            BufferedImage image = ImageIO.read(new File(imgPath));

            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(3));

            drawRectangle(g2d, xleft, ytop, xright, ybottom);

            g2d.dispose();

            String outputPath = imgPath;
            ImageIO.write(image, "png", new File(outputPath));

        } catch (IOException e) {
            System.err.println("Error processing " + imgPath);
            e.printStackTrace();
        }
    }

    private static void drawRectangle(Graphics2D g2d, int xleft, int ytop, int xright, int ybottom) {
        g2d.drawRect(xleft, ytop, xright - xleft, ybottom - ytop);
    }
}
