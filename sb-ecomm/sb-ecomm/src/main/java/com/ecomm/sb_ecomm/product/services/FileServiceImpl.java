package com.ecomm.sb_ecomm.product.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private static final int MAX_WIDTH = 6000;
    private static final int MAX_HEIGHT = 6000;
    private static final long MAX_PIXELS = 25_000_000L;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif"
    );

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif"
    );

    private void validateImage(MultipartFile image, String extension)
            throws IOException {

        try (InputStream input = image.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {

            if (imageInput == null) {
                throw new IllegalArgumentException("Invalid image data");
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(imageInput);

            if (!readers.hasNext()) {
                throw new IllegalArgumentException(
                        "File is not a supported image"
                );
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInput, true, true);

                // 5.1 Verify actual image format
                String format = reader.getFormatName()
                        .toLowerCase(Locale.ROOT);

                boolean formatMatches = switch (format) {
                    case "jpeg" -> extension.equals(".jpg")
                            || extension.equals(".jpeg");
                    case "png" -> extension.equals(".png");
                    case "gif" -> extension.equals(".gif");
                    default -> false;
                };

                if (!formatMatches) {
                    throw new IllegalArgumentException(
                            "Image content does not match its extension"
                    );
                }

                // 5.2 Check dimensions before decoding
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0 ||
                        width > MAX_WIDTH ||
                        height > MAX_HEIGHT ||
                        (long) width * height > MAX_PIXELS) {

                    throw new IllegalArgumentException(
                            "Image dimensions exceed allowed limits"
                    );
                }

                // 5.3 Actually decode the image
                BufferedImage decodedImage = reader.read(0);

                if (decodedImage == null) {
                    throw new IllegalArgumentException(
                            "Unable to decode image"
                    );
                }

                decodedImage.flush();

            } finally {
                reader.dispose();
            }
        }
    }

    @Override
    public String uploadImage(String path, MultipartFile image)
            throws IOException {

        // 1. Basic validation
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image cannot be empty");
        }

        // 2. File size validation
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Image exceeds maximum size of 10 MB"
            );
        }

        // 3. Filename and extension validation
        String originalFilename = image.getOriginalFilename();

        if (originalFilename == null ||
                originalFilename.isBlank() ||
                originalFilename.lastIndexOf('.') <= 0) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String extension = originalFilename.substring(
                originalFilename.lastIndexOf('.')
        ).toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported image extension"
            );
        }

        // 4. Declared MIME type validation
        String contentType = image.getContentType();

        if (contentType == null ||
                !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported MIME type");
        }

        // 5. Validate actual image content and dimensions
        validateImage(image, extension);

        // 6. Generate a safe filename
        String newFileName = UUID.randomUUID() + extension;

        // 7. Create destination directory
        Path uploadDirectory = Paths.get(path)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(uploadDirectory);

        Path destination = uploadDirectory
                .resolve(newFileName)
                .normalize();

        if (!destination.getParent().equals(uploadDirectory)) {
            throw new IOException("Invalid destination path");
        }

        // 8. Save without overwriting an existing file
        try (InputStream input = image.getInputStream()) {
            Files.copy(input, destination);

        } catch (IOException e) {
            Files.deleteIfExists(destination);
            throw e;
        }

        return newFileName;
    }


}