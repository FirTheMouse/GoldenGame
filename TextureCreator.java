package com.golden.game;

import javax.imageio.ImageIO;
import com.jhlabs.composite.*;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.File;
import java.util.Random;

public class TextureCreator {
    public static void createTestSkyboxTextures() {
        createSkyTexture("up", new Color(135, 206, 235));    // Sky blue
        createSkyTexture("down", new Color(82, 163, 42));    // Grass green
        createSkyTexture("north", new Color(135, 206, 235)); // Sky blue
        createSkyTexture("south", new Color(135, 206, 235)); // Sky blue
        createSkyTexture("east", new Color(135, 206, 235));  // Sky blue
        createSkyTexture("west", new Color(135, 206, 235));  // Sky blue
    }
    
    private static void createSkyTexture(String name, Color color) {
        try {
            BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(color);
            g2d.fillRect(0, 0, 512, 512);
            g2d.dispose();
            
            File dir = new File("assets/Textures/Sky");
            dir.mkdirs();
            System.out.println("Creating new directory");
            ImageIO.write(image, "png", new File(dir, name + ".png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void createTerrainTexture(String name, int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Random random = new Random(System.currentTimeMillis());
            
            // Create a larger-scale noise field
            float[][] baseNoise = new float[width/4][height/4];
            for (int x = 0; x < width/4; x++) {
                for (int y = 0; y < height/4; y++) {
                    baseNoise[x][y] = random.nextFloat();
                }
            }
            
            // Interpolate the noise to full size
            float[][] smoothNoise = new float[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    // Get the four nearest base noise points
                    int baseX = (x * 4) / width;
                    int baseY = (y * 4) / height;
                    baseX = Math.min(baseX, (width/4)-1);
                    baseY = Math.min(baseY, (height/4)-1);
                    
                    // Bilinear interpolation
                    float fx = ((x * 4) % width) / (float)width;
                    float fy = ((y * 4) % height) / (float)height;
                    
                    float top = lerp(baseNoise[baseX][baseY], 
                                   baseNoise[Math.min(baseX+1, (width/4)-1)][baseY], fx);
                    float bottom = lerp(baseNoise[baseX][Math.min(baseY+1, (height/4)-1)], 
                                      baseNoise[Math.min(baseX+1, (width/4)-1)][Math.min(baseY+1, (height/4)-1)], fx);
                    
                    smoothNoise[x][y] = lerp(top, bottom, fy);
                }
            }

            // Create the texture using the smooth noise
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    float value = smoothNoise[x][y];
                    
                    // Base grass color
                    int baseR = 150;
                    int baseG = 100;
                    int baseB = 50;
                    
                    // Variation color
                    int varR = 170;
                    int varG = 110;
                    int varB = 60;
                    
                    // Interpolate between colors
                    int r = (int)lerp(varR, baseR, value);
                    int g = (int)lerp(varG, baseG, value);
                    int b = (int)lerp(varB, baseB, value);
                    
                    image.setRGB(x, y, new Color(r, g, b).getRGB());
                }
            }

            File dir = new File("assets/Textures/Terrain");
            dir.mkdirs();
            ImageIO.write(image, "png", new File(dir, name + ".png"));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    public static void main(String[] args) {
       // createTestSkyboxTextures();
    	createTerrainTexture("grass", 512, 512);
    }
}
