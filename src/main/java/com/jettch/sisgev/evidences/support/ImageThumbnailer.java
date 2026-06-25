package com.jettch.sisgev.evidences.support;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Geração de miniatura (RN-023) usando apenas ImageIO (sem dependências externas). */
public final class ImageThumbnailer {

    private ImageThumbnailer() {
    }

    /**
     * Gera um JPEG reduzido com lado máximo {@code maxDimension}.
     * Retorna {@code null} se o conteúdo não for uma imagem decodificável.
     */
    public static byte[] toJpegThumbnail(byte[] original, int maxDimension) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
        if (source == null) {
            return null;
        }
        int w = source.getWidth();
        int h = source.getHeight();
        double scale = Math.min(1.0, (double) maxDimension / Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));

        BufferedImage thumb = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source.getScaledInstance(nw, nh, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(thumb, "jpeg", out);
        return out.toByteArray();
    }
}
