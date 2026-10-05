package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Clase que simula un Sistema Gestor de Base de Datos apoyado en un
 * fichero binario de acceso secuencial y registros de LONGITUD FIJA.
 */
public class FicheroSecuencial {

    private static final byte BYTE_ESPACIO = (byte) ' ';

    private final String rutaFichero;
    private final Map<String, Integer> esquemaCampos;
    private final String campoClave;

    private int longitudRegistro;
    private long numeroRegistros;
    private long registrosBorrados;

    /**
     * Constructor
     */
    public FicheroSecuencial(String rutaFichero, Map<String, Integer> esquemaCampos, String campoClave) throws IOException {
        this.rutaFichero = rutaFichero;
        this.esquemaCampos = esquemaCampos;
        this.campoClave = campoClave;
        this.numeroRegistros = 0;
        this.registrosBorrados = 0;
        this.longitudRegistro = 0;

        for (Map.Entry<String, Integer> campo : esquemaCampos.entrySet()) {
            this.longitudRegistro += campo.getValue();
        }

        File fichero = new File(rutaFichero);

        if (fichero.exists()) {
            this.numeroRegistros = fichero.length() / this.longitudRegistro;
        } else {
            fichero.createNewFile();
        }
    }

    /**
     * Busca secuencialmente un registro por su clave primaria.
     */
    public Map<String, String> recuperar(String valorClaveBuscado) throws IOException {
        try (FileInputStream fis = new FileInputStream(this.rutaFichero)) {
            int pos = 0;

            while (pos < this.numeroRegistros) {
                byte[] buffer = new byte[this.longitudRegistro];

                if (fis.read(buffer) < this.longitudRegistro) {
                    return null;
                }

                if (buffer[0] == 0) { // Registro borrado
                    pos++;
                    continue;
                }

                int offsetCampo = 0;
                String valorClaveLeido = null;

                for (Map.Entry<String, Integer> campo : esquemaCampos.entrySet()) {
                    if (campo.getKey().equals(this.campoClave)) {
                        valorClaveLeido = extraerStringDeBytes(buffer, offsetCampo, campo.getValue());
                        break;
                    }
                    offsetCampo += campo.getValue();
                }

                if (valorClaveBuscado.equals(valorClaveLeido)) {
                    Map<String, String> resultado = new LinkedHashMap<>();
                    offsetCampo = 0;

                    for (Map.Entry<String, Integer> campo : esquemaCampos.entrySet()) {
                        String valor = extraerStringDeBytes(buffer, offsetCampo, campo.getValue());
                        resultado.put(campo.getKey(), valor);
                        offsetCampo += campo.getValue();
                    }
                    return resultado;
                }
                pos++;
            }
            return null;
        }
    }

    /**
     * Inserta un nuevo registro garantizando ATOMICIDAD y RECUPERACIÓN DE ERRORES.
     *
     * @return
     *   >= 0 : Posición en la que se insertó el registro.
     *   -1   : Clave primaria duplicada.
     *   -2   : Error de validación en la longitud de uno de los campos.
     */
    public long insertar(Map<String, String> nuevoRegistro) throws IOException {
        String valorClave = nuevoRegistro.get(this.campoClave);

        if (recuperar(valorClave) != null) {
            return -1; // Código -1: Clave primaria duplicada
        }

        byte[] bufferRegistro = new byte[this.longitudRegistro];
        int offsetTemporal = 0;

        // CAPTURA INTERNA DE EXCEPCIÓN:
        // Validamos todos los campos en RAM. Si un campo es demasiado largo,
        // se captura la excepción aquí, se notifica y se devuelve -2 sin romper
        // la ejecución del programa ni tocar el fichero binario en disco.
        try {
            for (Map.Entry<String, Integer> campo : esquemaCampos.entrySet()) {
                String nombreCampo = campo.getKey();
                String valorCampo = nuevoRegistro.getOrDefault(nombreCampo, "");
                int bytesReservados = campo.getValue();

                byte[] bufferCampo = formatearABytesFijos(nombreCampo, valorCampo, bytesReservados);
                System.arraycopy(bufferCampo, 0, bufferRegistro, offsetTemporal, bytesReservados);
                offsetTemporal += bytesReservados;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("[Error de Inserción] " + e.getMessage());
            return -2; // Código -2: Fallo de validación de tamaño
        }

        // ESCRITURA EN DISCO (Solo si la validación completa fue exitosa)
        try (FileOutputStream fos = new FileOutputStream(rutaFichero, true)) {
            fos.write(bufferRegistro);
            this.numeroRegistros++;
            return this.numeroRegistros - 1;
        }
    }

    /**
     * Modifica el valor de un campo en un registro.
     * Captura internamente cualquier error de validación de tamaño y devuelve false.
     */
    public boolean modificar(String valorClave, String nombreCampo, String nuevoValor) throws IOException {
        if (nombreCampo.equals(this.campoClave)) {
            System.err.println("Operación denegada: No se permite modificar la clave primaria.");
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile(this.rutaFichero, "rws")) {
            int pos = 0;

            while (pos < this.numeroRegistros) {
                byte[] buffer = new byte[this.longitudRegistro];
                if (raf.read(buffer) < this.longitudRegistro) return false;

                if (buffer[0] != 0) {
                    int offset = 0;
                    String claveLeida = null;

                    for (Map.Entry<String, Integer> campo : esquemaCampos.entrySet()) {
                        if (campo.getKey().equals(this.campoClave)) {
                            claveLeida = extraerStringDeBytes(buffer, offset, campo.getValue());
                            break;
                        }
                        offset += campo.getValue();
                    }

                    if (valorClave.equals(claveLeida)) {
                        int offsetModificacion = 0;
                        for (Map.Entry<String, Integer> campo : esquemaCampos.entrySet()) {
                            if (campo.getKey().equals(nombreCampo)) {
                                long posicionFisica = ((long) pos * this.longitudRegistro) + offsetModificacion;

                                try {
                                    byte[] bufferNuevoValor = formatearABytesFijos(nombreCampo, nuevoValor, campo.getValue());
                                    raf.seek(posicionFisica);
                                    raf.write(bufferNuevoValor);
                                    return true;
                                } catch (IllegalArgumentException e) {
                                    System.err.println("[Error de Modificación] " + e.getMessage());
                                    return false; // Retorna false y el programa continúa
                                }
                            }
                            offsetModificacion += campo.getValue();
                        }
                    }
                }
                pos++;
            }
        }
        return false;
    }

    /**
     * Borrado lógico: Rellena todo el registro con ceros (byte 0).
     */
    public boolean borrar(String valorClave) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(this.rutaFichero, "rws")) {
            int pos = 0;
            while (pos < this.numeroRegistros) {
                byte[] buffer = new byte[this.longitudRegistro];
                if (raf.read(buffer) < this.longitudRegistro) return false;

                if (buffer[0] != 0) {
                    int offset = 0;
                    String claveLeida = null;

                    for (Map.Entry<String, Integer> campo : esquemaCampos.entrySet()) {
                        if (campo.getKey().equals(this.campoClave)) {
                            claveLeida = extraerStringDeBytes(buffer, offset, campo.getValue());
                            break;
                        }
                        offset += campo.getValue();
                    }

                    if (valorClave.equals(claveLeida)) {
                        raf.seek((long) pos * this.longitudRegistro);
                        byte[] bufferVacio = new byte[this.longitudRegistro];
                        Arrays.fill(bufferVacio, (byte) 0);

                        raf.write(bufferVacio);
                        this.registrosBorrados++;
                        return true;
                    }
                }
                pos++;
            }
        }
        return false;
    }

    /**
     * Elimina los registros borrados y reemplaza el fichero.
     */
    public int compactar() throws IOException {
        int numSuprimidos = 0;
        int nuevosRegistrosValidos = 0;
        File fTemp = File.createTempFile(rutaFichero, ".tmp");

        try (FileInputStream fis = new FileInputStream(rutaFichero);
             FileOutputStream fos = new FileOutputStream(fTemp)) {

            byte[] buffer = new byte[this.longitudRegistro];
            for (int pos = 0; pos < this.numeroRegistros; pos++) {
                if (fis.read(buffer) < this.longitudRegistro) break;

                if (buffer[0] == 0) {
                    numSuprimidos++;
                } else {
                    fos.write(buffer);
                    nuevosRegistrosValidos++;
                }
            }
        }

        File fOrig = new File(rutaFichero);
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        File fBackup = new File(rutaFichero + "." + timestamp + ".bak");

        if (fOrig.renameTo(fBackup) && fTemp.renameTo(fOrig)) {
            this.numeroRegistros = nuevosRegistrosValidos;
            this.registrosBorrados = 0;
            return numSuprimidos;
        }
        return -1;
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private byte[] formatearABytesFijos(String nombreCampo, String texto, int longitudFija) {
        byte[] bytesOriginales = texto.getBytes(StandardCharsets.UTF_8);

        if (bytesOriginales.length > longitudFija) {
            throw new IllegalArgumentException(
                    String.format("El campo '%s' ocupa %d bytes (máximo permitido: %d bytes).",
                            nombreCampo, bytesOriginales.length, longitudFija)
            );
        }

        byte[] bytesResultantes = new byte[longitudFija];
        Arrays.fill(bytesResultantes, BYTE_ESPACIO);
        System.arraycopy(bytesOriginales, 0, bytesResultantes, 0, bytesOriginales.length);

        return bytesResultantes;
    }

    private String extraerStringDeBytes(byte[] buffer, int offset, int longitud) {
        String texto = new String(buffer, offset, longitud, StandardCharsets.UTF_8);
        return texto.trim();
    }

    // =========================================================================
    // MAIN DE PRUEBA
    // =========================================================================

    public static void main(String[] args) {
        // Limpiamos fichero previo
        File fViejo = new File("alumnos_dam.dat");
        if (fViejo.exists()) fViejo.delete();

        try {
            Map<String, Integer> esquema = new LinkedHashMap<>();
            esquema.put("DNI", 9);
            esquema.put("NOMBRE", 32);
            esquema.put("CP", 5);

            FicheroSecuencial fs = new FicheroSecuencial("alumnos_dam.dat", esquema, "DNI");

            // -----------------------------------------------------------------
            // PRUEBA 1: Registro correcto
            // -----------------------------------------------------------------
            System.out.println("--- 1. INSERCIÓN CORRECTA ---");
            Map<String, String> reg1 = new LinkedHashMap<>();
            reg1.put("DNI", "12345678Z");
            reg1.put("NOMBRE", "LÓPEZ YÁÑEZ");
            reg1.put("CP", "29730");

            long resultado1 = fs.insertar(reg1);
            if (resultado1 >= 0) {
                System.out.println("Registro insertado en posición: " + resultado1);
            }

            // -----------------------------------------------------------------
            // PRUEBA 2: Intento con texto excedido (Demostración de recuperación)
            // -----------------------------------------------------------------
            System.out.println("\n--- 2. INSERCIÓN ERRÓNEA (NOMBRE DEMASIADO LARGO) ---");
            Map<String, String> regErroneo = new LinkedHashMap<>();
            regErroneo.put("DNI", "87654321A");
            regErroneo.put("NOMBRE", "NOMBRE EXAGERADAMENTE LARGO QUE SUPERA LOS TREINTA Y DOS BYTES DE LÍMITE");
            regErroneo.put("CP", "28001");

            // No requiere try-catch alrededor. Simplemente comprobamos el código de retorno.
            long resultadoErroneo = fs.insertar(regErroneo);

            if (resultadoErroneo == -2) {
                System.out.println("El programa detectó la longitud excesiva y se recuperó limpiamente sin romper la ejecución.");
            }

            // -----------------------------------------------------------------
            // PRUEBA 3: Continuación normal de ejecución tras el error
            // -----------------------------------------------------------------
            System.out.println("\n--- 3. CONTINUACIÓN NORMAL DE LA EJECUCIÓN ---");
            Map<String, String> reg2 = new LinkedHashMap<>();
            reg2.put("DNI", "87654321A");
            reg2.put("NOMBRE", "GARCÍA MÁRQUEZ");
            reg2.put("CP", "28001");

            long resultado2 = fs.insertar(reg2);
            if (resultado2 >= 0) {
                System.out.println("Nuevo registro insertado en posición: " + resultado2);
            }

            System.out.println("\nContenido del fichero recuperado:");
            System.out.println("DNI 12345678Z: " + fs.recuperar("12345678Z"));
            System.out.println("DNI 87654321A: " + fs.recuperar("87654321A"));

        } catch (IOException e) {
            System.err.println("Error de entrada/salida: " + e.getMessage());
        }
    }
}