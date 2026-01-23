package com.kaymlyn.planeteater.rendering;

import com.kaymlyn.planeteater.simulation.celestial.Orbiter;
import com.kaymlyn.planeteater.simulation.celestial.OrbitingBody;
import com.kaymlyn.planeteater.simulation.celestial.OrbitalSystem;
import com.kaymlyn.planeteater.simulation.physics.PhysicsConstants;
import com.kaymlyn.planeteater.simulation.vehicles.Spacecraft;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;


public class OrbitalSystemRenderer {

    private final OrbitalSystem system;
    private int i;
    private final int scalar;
    private final double visibleAU;

    private static final int imageType = TYPE_INT_RGB;
    public OrbitalSystemRenderer(OrbitalSystem system, double visibleAU) {
        scalar = 80;
        i = 0;
        this.system = system;
        this.visibleAU = visibleAU;
        File directory = new File("orbits");
        if(!directory.exists()) {
            directory.mkdir();
        }
        for(File file : Objects.requireNonNull(directory.listFiles())) {
            file.delete();
        }
    }

    public void render(boolean saveImage, double maxAUVisible) throws IOException {
        BufferedImage frame = render(system, scalar, maxAUVisible);
        renderInfo(frame.createGraphics(),"Day " + (int)(system.getCurrentTime()/PhysicsConstants.SECONDS_PER_DAY)
                + " Hour " + (int)((system.getCurrentTime()%PhysicsConstants.SECONDS_PER_DAY)/3600),0,16);

        if(saveImage) {
            saveImage("Frame-" + i, frame);
        }
        i++;
    }

    public void renderInfo(Graphics2D canvas, String info, int x, int y) {
        canvas.setColor(Color.WHITE);
        canvas.drawString(info, x, y);
    }

    private BufferedImage render(OrbitalSystem system, int scalar, double maxAUVisible) {
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

        canvas.setColor(Color.YELLOW);
        canvas.fillOval(width*scalar/2, height*scalar/2, 8,8);

        double canvasScale = PhysicsConstants.AU*(maxAUVisible * 3 /4);

        for(Orbiter orbiter : system.getOrbiters().values()) {
            canvas.setColor(Color.WHITE);

            if(!orbiter.getId().contains("Belt")) {
                canvas.setColor(
                        new Color(
                                random.nextInt(0,256),
                                255,
                                0
                        )
                );
            }
            if(orbiter.getId().contains("KHI")) {
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
            if(orbiter instanceof OrbitingBody && ((OrbitingBody)orbiter).getRadius() > 50000) {
                size = 3;
            } else {
                size = 2;
            }
            double xRaw = orbiter.getPosition().getX()/canvasScale;
            double yRaw = orbiter.getPosition().getY()/canvasScale;
            rectangle.setRect(xRaw * adjustedAU + ((double) (width * scalar) /2),yRaw * adjustedAU + ((double)(height*scalar)/2),size, size);
            canvas.fill(rectangle);
            if(!orbiter.getId().contains("Belt")) {
                renderInfo(canvas,
                        orbiter.getId(),
                        (int) (xRaw * adjustedAU + ((double) (width * scalar) / 2) + 3),
                        (int) (yRaw * adjustedAU + ((double) (height * scalar) / 2)));
            }

        }
        for(Spacecraft ship : system.getSpacecraftInTransit().values()) {
            if(ship.getTelemetry() != null && ship.getCurrentTravelCycle() < ship.getTelemetry().size()) {
                System.out.println("origin :" + ship.getSystem().getOrbiter("KHI Central Mind").getPosition());
                System.out.println("positi :" + ship.getTelemetry().get(ship.getCurrentTravelCycle()).getPosition());
                System.out.println("destin :" + ship.getSystem().getOrbiter("Theseus").getPosition());
            }
            if(ship.getState() != Spacecraft.SpacecraftState.DOCKED) {
                canvas.setColor(
                        new Color(
                                255,
                                0,
                                0
                        )
                );
                double xRaw;
                double yRaw;
                if(ship.getTelemetry() != null && ship.getCurrentTravelCycle() < ship.getTelemetry().size()) {
                    xRaw = ship.getTelemetry().get(ship.getCurrentTravelCycle()).getPosition().getX();
                    yRaw = ship.getTelemetry().get(ship.getCurrentTravelCycle()).getPosition().getY();
                } else if (ship.getOrbiting() != null){
                    xRaw = ship.getOrbiting().getPosition().getX()/canvasScale;
                    yRaw = ship.getOrbiting().getPosition().getY()/canvasScale;
                } else {
                    xRaw = ship.getPosition().getX()/canvasScale;
                    yRaw = ship.getPosition().getY()/canvasScale;
                }
                Rectangle rectangle = new Rectangle();
                int size = 1;

                System.out.println("Ship coords: " + (xRaw * adjustedAU + ((double) (width * scalar) / 2)) + " : " + (yRaw * adjustedAU + ((double) (height * scalar) / 2)));
                rectangle.setRect(xRaw * adjustedAU + ((double) (width * scalar) / 2), yRaw * adjustedAU + ((double) (height * scalar) / 2), size, size);
                canvas.fill(rectangle);
                renderInfo(canvas,
                        ship.getId() + " " + ship.getState(),
                        (int) (xRaw * adjustedAU + ((double) (width * scalar) / 2) -10),
                        (int) (yRaw * adjustedAU + ((double) (height * scalar) / 2)));
            }
        }

        return image;
    }

    private double scaleAUToCanvas(int height, int width) {
        int min = Math.min(height,width);
        return scalar * (min/4.0);
    }

    private void saveImage(String name, BufferedImage currentFrame) throws IOException {

        File output = new File("orbits/" + name + ".png");
        ImageIO.write(currentFrame, "PNG", output);
    }

    public void renderVideoFromImages(double scalar) throws IOException {
        File output = new File("orbits/test.mp4");
        AffineTransform scale = new AffineTransform();
        BufferedImage reference = ImageIO.read(new File("orbits/Frame-0.png"));
        int scaleWidth = (int)Math.round(reference.getWidth()*scalar);
        int scaleHeight = (int)Math.round(reference.getHeight()*scalar);
        scale.scale(scaleWidth,scaleHeight);
        SequenceEncoder enc = SequenceEncoder.createWithFps(NIOUtils.writableChannel(output), Rational.R(30,1));
        int i=0;
        File inputFrame = new File("orbits/Frame-" + i + ".png");
        while(inputFrame.exists()) {
            try {
                enc.encodeNativeFrame(scaleImage(inputFrame, scaleWidth, scaleHeight));
            } catch (IOException e) {
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
        int i = 0;
        File input = new File("orbits/Frame-" + i + ".png");
        while(input.exists()) {
            BufferedImage image = ImageIO.read(input);
            try {
                enc.encodeNativeFrame(AWTUtil.fromBufferedImage(image, ColorSpace.RGB));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            i++;
            input = new File("orbits/Frame-" + i + ".png");
        }
        enc.finish();

    }
}
