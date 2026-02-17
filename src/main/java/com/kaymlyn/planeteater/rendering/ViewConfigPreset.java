package com.kaymlyn.planeteater.rendering;

import com.kaymlyn.planeteater.simulation.physics.Vector3D;

import java.awt.*;

public class ViewConfigPreset {
    public static final ViewConfig TOP_DOWN = ViewConfig.builder()
            .gridConfig(
                    GridConfig.builder()
                    .showGraduatedAxes(true)
                    .xAxisConfig(AxisConfig.standard(new Color(160, 40, 40)))
                    .yAxisConfig(AxisConfig.standard(new Color(40, 160, 40)))
                    .zAxisConfig(AxisConfig.disabled())
                    .showEclipticGrid(false)
                            .build())
            .labelConfig(
                    LabelConfig.builder()
                            .showPlanets(true)
                            .showSatellites(true)
                            .showSpacecraft(true)
                            .build())
            .objectSizeStrategy(ObjectSizeStrategy.CONSTANT)
            .rotation(Vector3D.UNIT_Z.multiply(-Math.PI/2))
            .showTimestamp(true)
            .build();

    public static final ViewConfig SIDE_ON = ViewConfig.builder()
            .gridConfig(
                    GridConfig.builder()
                            .showGraduatedAxes(true)
                            .xAxisConfig(AxisConfig.standard(new Color(160, 40, 40)))
                            .yAxisConfig(AxisConfig.disabled())
                            .zAxisConfig(AxisConfig.standard(new Color(40, 40, 160)))
                            .showEclipticGrid(false)
                            .build())
            .labelConfig(
                    LabelConfig.builder()
                            .showPlanets(true)
                            .showSatellites(true)
                            .showSpacecraft(true)
                            .build())
            .objectSizeStrategy(ObjectSizeStrategy.CONSTANT)
            .rotation(Vector3D.UNIT_X.multiply(Math.PI/2))
            .build();

    public static final ViewConfig SIDE_ON_90 = ViewConfig.builder()
            .gridConfig(
                    GridConfig.builder()
                            .showGraduatedAxes(true)
                            .xAxisConfig(AxisConfig.disabled())
                            .yAxisConfig(AxisConfig.standard(new Color(40, 160, 40)))
                            .zAxisConfig(AxisConfig.standard(new Color(40, 40, 160)))
                            .showEclipticGrid(false)
                            .build())
            .labelConfig(
                    LabelConfig.builder()
                            .showPlanets(true)
                            .showSatellites(true)
                            .showSpacecraft(true)
                            .build())
            .objectSizeStrategy(ObjectSizeStrategy.CONSTANT)
            .rotation(new Vector3D(Math.PI/2,0, -Math.PI/2))
            .labelConfig(LabelConfig.majorBodiesOnly())
            .build();

    public static final ViewConfig ANGLED = ViewConfig
            .builder()
            .gridConfig(GridConfig.full())
            .labelConfig(
                    LabelConfig.builder().showPlanets(true).showSatellites(true).showSpacecraft(true).build())
            .objectSizeStrategy(ObjectSizeStrategy.CONSTANT)
            .rotation(AngledViewPreset.THREE_QUARTER.toRotation())
            .build();
}
