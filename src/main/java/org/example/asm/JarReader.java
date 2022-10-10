package org.example.asm;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class JarReader{

    public static Stream<ClassNode> loadClassNode(JarFile jar) throws IOException {
            return jar.stream().parallel()
                    .filter(z -> z.getName().endsWith(".class"))
                    .map(z -> {
                        try (InputStream jis = jar.getInputStream(z)) {
                            byte[] bytes = IOUtils.toByteArray(jis);
                            String cafebabe = String.format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]);
                            if (!cafebabe.equalsIgnoreCase("cafebabe")) {
                                throw new IllegalStateException(String.format("'%s' doesn't have a valid magic", z.getName()));
                            }
                            return getNode(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    });
    }

    private static ClassNode getNode(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        try {
            cr.accept(cn, ClassReader.EXPAND_FRAMES);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cn;
    }

}