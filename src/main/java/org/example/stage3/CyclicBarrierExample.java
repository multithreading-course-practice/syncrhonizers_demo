package org.example.stage3;

import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class CyclicBarrierExample {

    private final static int MAX_ITERATION = 512;

    private final static float HUE_SHIFT = 0.14f;

    private static CyclicBarrier barrier;

    private static void nanoWait(int nano) {
        long end = System.nanoTime() + nano;
        while (System.nanoTime() < end) ;
    }

    public static class JuliaSetRender extends JPanel {

        private final BufferedImage image;

        public JuliaSetRender(int width, int height) {
            setPreferredSize(new Dimension(width, height));
            setBackground(Color.GRAY);
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }

        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponents(graphics);
            Graphics2D graphics2D = (Graphics2D) graphics;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.drawImage(image, 0, 0, null);
        }

        public void fillRow(ComputedRow row) {
            for (int i = 0; i < row.columns.length; i++) {
                int columnRes = row.columns[i];
                float hue = columnRes * 1.0f / MAX_ITERATION;
                if (hue + HUE_SHIFT > 1) {
                    hue += HUE_SHIFT - 1;
                } else {
                    hue += HUE_SHIFT;
                }

                int color = Color.HSBtoRGB(hue, 1, 1/*columnRes > 1 ? 1 : 0*/);
                image.setRGB(i, row.rowNumber, color);
            }

        }

    }

    public static class ComputedRow {
        int rowNumber;
        int[] columns;

        public ComputedRow(int rowNumber, int[] columns) {
            this.rowNumber = rowNumber;
            this.columns = columns;
        }


    }

    static final int width = 1024;
    static final int height = 768;
    private static final double CX = -0.7;
    private static final double CY = 0.27015;
    static volatile int[] lineResults = new int[width];
    static volatile AtomicInteger rowNumber = new AtomicInteger(0);
    static CountDownLatch lineRenderedLatch = new CountDownLatch(height);


    public static void main(String[] args) {

        JuliaSetRender render = new JuliaSetRender(width, height);
        JFrame jFrame = new JFrame();
        setupFrame(render, jFrame);

        int threadsCount = 4;
        barrier = new CyclicBarrier(threadsCount, () -> {
            int[] result = Arrays.copyOf(lineResults, width);
            render.fillRow(new ComputedRow(rowNumber.getAndIncrement(), result));
            jFrame.validate();
            jFrame.repaint();
            lineRenderedLatch.countDown();
            log.info("Line compute completed");

        });
        ExecutorService pool = Executors.newFixedThreadPool(threadsCount);

        final int segmentWidth = width / threadsCount;
        for (int segment = 0; segment < threadsCount; segment++) {
            pool.execute(getWorker(segmentWidth, segment));
        }

        while (lineRenderedLatch.getCount() > 0) {
            try {
                lineRenderedLatch.await();
            } catch (InterruptedException e) {
                //
            }
        }
        log.info("Computing finished");
        pool.shutdown();
    }

    private static Runnable getWorker(final int segmentWidth, final int segmentIndex) {
        return () -> {
            while (rowNumber.get() < height) {
                int lastRowNumber = rowNumber.get();
                log.info("Segment {}@{} start compute", segmentIndex, lastRowNumber);
                calculations(segmentWidth, segmentIndex, lastRowNumber);
                while (lastRowNumber == rowNumber.get()) {
                    try {
                        log.info("Segment {}@{} finish compute, wait others", segmentIndex, lastRowNumber);
                        barrier.await();
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    } catch (BrokenBarrierException e) {
                        //return;
                    }
                }
            }
        };
    }

    private static void setupFrame(JuliaSetRender render, JFrame jFrame) {
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setTitle("Parallel Julia Set");
        jFrame.setResizable(false);
        jFrame.add(render, BorderLayout.CENTER);
        jFrame.pack();
        jFrame.setLocationRelativeTo(null);
        jFrame.setVisible(true);
    }

    private static void calculations(int segmentWidth, int segmentIndex, int lastRowNumber) {
        for (int column = 0; column < segmentWidth; column++) {
            int x = segmentIndex * segmentWidth + column;
            int y = lastRowNumber;
            double zx = 1.5 * (x - width / 2.0f) / (0.5 * width);
            double zy = (y - height / 2.f) / (0.5 * height);
            int i = MAX_ITERATION;
            while (zx * zx + zy * zy < 40 && i > 1) {
                double tmp = zx * zx - zy * zy + CX;
                zy = 2.0 * zx * zy + CY;
                zx = tmp;
                i--;
                nanoWait(100);
            }
            lineResults[x] = i;
        }
    }
}
