package com.kaymlyn.planeteater.rendering;

import com.kaymlyn.planeteater.simulation.celestial.CelestialBody;
import com.kaymlyn.planeteater.simulation.physics.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;


public class OrbitalSystemRenderer {

    private final OrbitalSystem system;
    private int i;
    private final int scalar;
    private final LinkedHashMap<String,BufferedImage> frames;

    private static final int imageType = TYPE_INT_RGB;
    public OrbitalSystemRenderer(OrbitalSystem system) {
        scalar = 80;
        i = 0;
        this.system = system;
        frames = new LinkedHashMap<>();
        File directory = new File("orbits");
        if(!directory.exists()) {
            directory.mkdir();
        }
        for(File file : Objects.requireNonNull(directory.listFiles())) {
            file.delete();
        }
    }

    public void render(boolean saveImage, int maxAUVisible) throws IOException {
        frames.put("Frame-" + i, render(system, scalar, maxAUVisible));
        renderInfo("Day " + (int)(system.getCurrentTime()/PhysicsConstants.SECONDS_PER_DAY)
                + " Hour " + (int)((system.getCurrentTime()%PhysicsConstants.SECONDS_PER_DAY)/3600));

        if(saveImage) {
            saveImage("Frame-" + i);
        }
        i++;
    }

    public void renderInfo(String info) {
        Map.Entry<String, BufferedImage> frame = frames.lastEntry();
        Graphics2D canvas = frame.getValue().createGraphics();
        canvas.setColor(Color.WHITE);
        canvas.drawString(info,0,16);
    }

    private BufferedImage render(OrbitalSystem system, int scalar, int maxAUVisible) {
        int height = 9;
        int width = 16;

        Random random = new Random(scalar);

        double adjustedAU = scaleAUToCanvas(9,16);
        BufferedImage image = new BufferedImage(
                width * scalar,
                height * scalar,
                imageType
        );
        Graphics2D canvas = image.createGraphics();
        canvas.setBackground(Color.BLACK);

        for(CelestialBody body : system.getOrbitingBodies()) {
            canvas.setColor(Color.WHITE);

            if(body.getId().contains("00"))
            {
                canvas.setColor(
                        new Color(
                                random.nextInt(0,256),
                                255,
                                0
                        )
                );
            }
            if(body.getId().contains("50"))
            {
                canvas.setColor(
                        new Color(
                                random.nextInt(0,256),
                                0,
                                255
                        )
                );
            }
            Rectangle rectangle = new Rectangle();
            int size;
            if(body.getRadius() > 50000) {
                size = 3;
            } else {
                size = 2;
            }
            double xRaw = body.getPosition().getX()/(PhysicsConstants.AU*((double) (maxAUVisible * 3) /4));
            double yRaw = body.getPosition().getY()/(PhysicsConstants.AU*((double) (maxAUVisible * 3) /4));
            rectangle.setRect(xRaw * adjustedAU + ((double) (width * scalar) /2),yRaw * adjustedAU + ((double)(height*scalar)/2),size, size);
            canvas.fill(rectangle);
        }
        canvas.setColor(Color.YELLOW);

        canvas.fillOval(width*scalar/2, height*scalar/2, 8,8);

        return image;
    }

    public BufferedImage getCurrentFrame() {
        return frames.lastEntry().getValue();
    }

    private double scaleAUToCanvas(int height, int width) {
        int min = Math.min(height,width);
        return scalar * (min/4.0);
    }

    private void saveImage(String name) throws IOException {

        File output = new File("orbits/" + name + ".png");
        ImageIO.write(getCurrentFrame(), "PNG", output);
    }

    public void renderVideoFromImages(double scalar) throws IOException {
        File output = new File("orbits/test.mp4");
        AffineTransform scale = new AffineTransform();
        BufferedImage reference = ImageIO.read(new File("orbits/Frame-0.png"));
        int scaleWidth = (int)Math.round(reference.getWidth()*scalar);
        int scaleHeight = (int)Math.round(reference.getHeight()*scalar);
        scale.scale(scaleWidth,scaleHeight);
        AffineTransformOp scaleOp = new AffineTransformOp(scale, AffineTransformOp.TYPE_BILINEAR);
        SequenceEncoder enc = SequenceEncoder.createWithFps(NIOUtils.writableChannel(output), Rational.R(30,1));
        int i=0;
        File inputFrame = new File("orbits/Frame-" + i + ".png");
        while(inputFrame.exists()) {
            try {
                enc.encodeNativeFrame(scaleImage(inputFrame, scaleWidth, scaleHeight));
            } catch (IOException e) {
                System.out.println("exception");
                throw new RuntimeException(e);
            }
            inputFrame = new File("orbits/Frame-" + (i++) + ".png");
        }
        enc.finish();
    }

    private static Picture scaleImage(File source, int scaleWidth, int scaleHeight) throws IOException {
        Image scaledImage = ImageIO.read(source).getScaledInstance(scaleWidth,scaleHeight,Image.SCALE_SMOOTH);
        BufferedImage scaled = new BufferedImage(scaleWidth, scaleHeight,imageType);
        Graphics2D g2d = scaled.createGraphics();
        g2d.drawImage(scaledImage,0,0,null);
        g2d.dispose();
        return AWTUtil.fromBufferedImage(scaled, ColorSpace.RGB);
    }

    public void renderVideo() throws IOException {
        File output = new File("orbits/test.mp4");
        SequenceEncoder enc = SequenceEncoder.createWithFps(NIOUtils.writableChannel(output), Rational.R(30,1));
        frames.sequencedValues().forEach(image -> {
            try {
                enc.encodeNativeFrame(AWTUtil.fromBufferedImage(image, ColorSpace.RGB));
            } catch (IOException e) {
                System.out.println("exception");
                throw new RuntimeException(e);
            }
        });
        enc.finish();

    }
}
