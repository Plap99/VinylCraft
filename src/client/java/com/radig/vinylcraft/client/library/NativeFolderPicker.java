package com.radig.vinylcraft.client.library;

import java.nio.file.Path;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NativeFileDialog;

public final class NativeFolderPicker {

    private NativeFolderPicker() {
    }


    public static Path pickFolder() {

        try (
                MemoryStack stack =
                        MemoryStack.stackPush()
        ) {

            PointerBuffer outPath =
                    stack.mallocPointer(1);

            int result =
                    NativeFileDialog.NFD_PickFolder(
                            outPath,
                            (CharSequence) null
                    );

            if (
                    result
                    == NativeFileDialog.NFD_OKAY
            ) {

                long pathPointer =
                        outPath.get(0);

                String selectedPath =
                        org.lwjgl.system.MemoryUtil
                                .memUTF8(pathPointer);

                NativeFileDialog.NFD_FreePath(
                        pathPointer
                );

                if (
                        selectedPath == null
                        || selectedPath.isBlank()
                ) {
                    return null;
                }

                return Path.of(
                        selectedPath
                );
            }

            if (
                    result
                    == NativeFileDialog.NFD_CANCEL
            ) {
                return null;
            }

            System.err.println(
                    "[VinylCraft] Error en selector "
                            + "de carpeta: "
                            + NativeFileDialog.NFD_GetError()
            );

            return null;

        } catch (Exception exception) {

            System.err.println(
                    "[VinylCraft] No se pudo abrir "
                            + "el selector de carpetas."
            );

            exception.printStackTrace();

            return null;
        }
    }
}