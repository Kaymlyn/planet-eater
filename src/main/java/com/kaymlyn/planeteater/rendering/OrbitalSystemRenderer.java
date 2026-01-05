package com.kaymlyn.planeteater.rendering;

import com.kaymlyn.planeteater.simulation.celestial.CelestialBody;
import com.kaymlyn.planeteater.simulation.physics.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Rational;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;


public class OrbitalSystemRenderer {

    private OrbitalSystem system;
    private int i;
    private int scalar;
    private LinkedHashMap<String,BufferedImage> frames;
    public OrbitalSystemRenderer(OrbitalSystem system) throws IOException {
        scalar = 80;
        i = 0;
        this.system = system;
        frames = new LinkedHashMap<>();
        File directory = new File("orbits");
        if(!directory.exists()) {
            directory.mkdir();
        }
        for(File file : directory.listFiles()) {
            file.delete();
        }
    }

    public void render(boolean saveimage, int maxAUVisible) throws IOException {
        frames.put("Frame-" + i, render(system, scalar, maxAUVisible));
        renderInfo("Day " + (int)(system.getCurrentTime()/PhysicsConstants.SECONDS_PER_DAY)
                + " Hour " + (int)((system.getCurrentTime()%PhysicsConstants.SECONDS_PER_DAY)/3600));
        i++;
        if(saveimage) {
            saveImage("Frame-" + i);
        }
    }

    public void renderInfo(String info) throws IOException {
        Map.Entry<String, BufferedImage> frame = frames.lastEntry();
        Graphics2D canvas = frame.getValue().createGraphics();
        canvas.setColor(Color.WHITE);
        canvas.drawString(info,0,16);
    }

    private BufferedImage render(OrbitalSystem system, int scalar, int maxAUVisible) {
        int height = 9;
        int width = 16;

        double adjustedAU = scaleAUToCanvas(9,16);
        BufferedImage image = new BufferedImage(
                width * scalar,
                height * scalar,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D canvas = image.createGraphics();
        canvas.setBackground(Color.BLACK);
        canvas.setColor(Color.WHITE);

        for(CelestialBody body : system.getOrbitingBodies()) {
            Rectangle rectangle = new Rectangle();
            int size;
            if(body.getRadius() > 50000) {
                size = 3;
            } else {
                size = 2;
            }
            double xRaw = body.getPosition().getX()/(PhysicsConstants.AU*(maxAUVisible*3/4));
            double yRaw = body.getPosition().getY()/(PhysicsConstants.AU*(maxAUVisible*3/4));
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

    public void renderVideoFromImages() throws IOException {
        File output = new File("orbits/test.mp4");
        SequenceEncoder enc = SequenceEncoder.createWithFps(NIOUtils.writableChannel(output), Rational.R(30,1));
        int i=1;
        File inputFrame = new File("orbits/Frame-" + i + ".png");
        while(inputFrame.exists()) {
            try {
                enc.encodeNativeFrame(AWTUtil.fromBufferedImage(ImageIO.read(inputFrame), ColorSpace.RGB));
            } catch (IOException e) {
                System.out.println("exception");
                throw new RuntimeException(e);
            }
            inputFrame = new File("orbits/Frame-" + i++ + ".png");
        }
        enc.finish();
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
