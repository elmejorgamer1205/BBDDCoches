package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GestionCoches {

    private static final int BYTES_MATRICULA = 7;
    private static final int BYTES_MARCA = 32;
    private static final int BYTES_MODELO = 32;
    private static final int TAMANO_REGISTRO = BYTES_MATRICULA + BYTES_MARCA + BYTES_MODELO;
    Set<byte[]> listaDeRegistros = new HashSet<>();
    private static final byte BYTE_ESPACIO = (byte) ' ';
    private static final String ARCHIVO_BD = "coches.dat";
    File archivo = new File(ARCHIVO_BD);

    public static void main(String[] args) {
        boolean salir = false;
        while (!salir) {
            System.out.println("\n--- GESTIÓN DE COCHES ---");
            System.out.println("1. Cargar información de CSV");
            System.out.println("2. Insertar coche en posición concreta");
            System.out.println("3. Ordenar fichero por matrícula");
            System.out.println("4. Borrar un registro");
            System.out.println("5. Modificar un registro");
            System.out.println("6. Mostrar todos los coches (Extra, para comprobar)");
            System.out.println("7. Salir");

            try {
                int opcion = MiEntradaSalida.leerEntero("Elige una opción: ");
                switch (opcion) {
                    case 1:
                        // permite que si pones la ruta o le das a la tecla intro se cargue el archivo csv
                        String ruta = MiEntradaSalida.leerLinea("Ruta del CSV [src/BBDD Coches.csv]: ").trim();
                        // si no se escribió una ruta alternativa, carga el CSV del proyecto.
                        cargarCSV(ruta.isEmpty() ? "src/BBDD Coches.csv" : ruta);
                        break;
                    case 2:
                        //introduce un coche en una posición concreta al .dat
                        insertar(MiEntradaSalida.leerLinea("Matrícula: ").trim(), MiEntradaSalida.leerLinea("Marca: ").trim(), MiEntradaSalida.leerLinea("Modelo: ").trim(), MiEntradaSalida.leerEntero("Posición (desde 0): "));
                        break;
                    case 3:
                        // ordena todos los coches por su matricula
                        ordenar();
                        break;
                    case 4:
                        // el usuario elige si localizar el registro de los coches por clave o por su índice
                        int modo = MiEntradaSalida.leerEntero("1. Matrícula, 2. Posición: ");
                        // busca por la clave primaria matrícula
                        if (modo == 1) borrarPorMatricula(MiEntradaSalida.leerLinea("Matrícula: ").trim());
                        // borra directamente por su índice físico
                        else if (modo == 2) borrarPorPosicion(MiEntradaSalida.leerEntero("Posición (desde 0): "));
                        // Cualquier otro valor no coincide con una operación de borrado
                        else System.out.println("Opción no válida.");
                        break;
                    case 5:
                        // se recogen únicamente marca y modelo para no cambiar la matrícula.
                        int posicion = MiEntradaSalida.leerEntero("Posición (desde 0): ");
                        modificar(posicion, MiEntradaSalida.leerLinea("Nueva marca: ").trim(), MiEntradaSalida.leerLinea("Nuevo modelo: ").trim());
                        break;
                    case 6:
                        // Lista todos los coches actuales.
                        listarCoches();
                        break;
                    case 7:
                        // Cambia la condición del bucle para cerrar el menú.
                        salir = true;
                        System.out.println("Saliendo, ten un buen día!");
                        break;
                    default:
                        // Informa cuando el usuario introduce una opción fuera del menú.
                        System.out.println("Opción incorrecta.");
                }
            } catch (NumberFormatException e) {
                // Informa del error numérico sin terminar el programa.
                System.out.println("Introduce un número válido.");
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("No se pudo completar la operación: " + e.getMessage());
            }
        }
    }

    /** Carga los datos del archivo csv al fichero binario.
     * @param nombreArchivoCsv Ruta del archivo CSV que se va a cargar.
     */
    private static void cargarCSV(String nombreArchivoCsv) {
        // Crea una referencia al archivo de texto que se va a importar.
        File archivoCsv = new File(nombreArchivoCsv);
        // Evita intentar abrir una ruta que no exista.
        if (!archivoCsv.exists()) {
            System.out.println("Error: El archivo csv no se encuentra.");
            return;
        }

        // El lector con buffer procesa el CSV línea por línea y RandomAccessFile escribe el binario.
        try (BufferedReader br = new BufferedReader(new FileReader(archivoCsv));
             RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {

            // Guarda cada línea leída del CSV.
            String linea;
            // Cuenta los registros nuevos que se incorporen al fichero.
            int contador = 0;
            // La primera fila es la cabecera de nombres de columna y no es un coche.
            br.readLine();
            // Leemos el CSV texto a texto
            while ((linea = br.readLine()) != null) {
                // Acepta CSV separados por coma o por punto y coma.
                String[] partes = linea.split("[,;]");
                // Solo se procesa la fila si contiene matrícula, marca y modelo.
                if (partes.length >= 3) {
                    // Quita los espacios que rodean a cada valor del CSV.
                    String matricula = partes[0].trim();
                    String marca = partes[1].trim();
                    String modelo = partes[2].trim();

                    // Evitamos truncar datos que no quepan en los campos fijos del fichero.
                    validarRegistro(matricula, marca, modelo);

                    // Usamos al ayudante para ver si ya existe
                    if (!existeMatricula(raf, matricula)) {

                        // Si no existe, vamos al final del archivo para añadir el nuevo coche
                        raf.seek(raf.length());

                        // Usamos la herramienta segura para guardar los bytes exactos
                        escribirCampoFijo(raf, matricula, BYTES_MATRICULA);
                        escribirCampoFijo(raf, marca, BYTES_MARCA);
                        escribirCampoFijo(raf, modelo, BYTES_MODELO);

                        // Actualiza el total de coches añadidos correctamente.
                        contador++;
                    } else {
                        // Omite claves duplicadas para mantener única la matrícula.
                        System.out.println("Aviso: La matrícula " + matricula + " ya existe. Se omite.");
                    }
                }
            }
            // Resume cuántos registros se añadieron al fichero.
            System.out.println("Carga completada. Se han insertado " + contador + " coches.");

        } catch (IOException e) {
            // Captura fallos al leer el CSV o escribir la base de datos.
            System.out.println("Error de entrada/salida: " + e.getMessage());
        }
    }

    /** Convierte un campo a su representación de bytes de longitud fija.
     * @param nombreCampo Nombre descriptivo del campo, usado en el mensaje de error.
     * @param texto Texto que se convertirá a bytes.
     * @param longitudFija Número de bytes que debe ocupar el campo.
     * @return Arreglo con el texto codificado y completado con espacios.
     */
    private byte[] formatearABytesFijos(String nombreCampo, String texto, int longitudFija) {
        // Codifica el texto en UTF-8 antes de medirlo y guardarlo.
        byte[] bytesOriginales = texto.getBytes(StandardCharsets.UTF_8);

        // Rechaza valores que no caben para evitar alterar el tamaño del registro.
        if (bytesOriginales.length > longitudFija) {
            throw new IllegalArgumentException(
                    String.format("El campo '%s' ocupa %d bytes (máximo permitido: %d bytes).",
                            nombreCampo, bytesOriginales.length, longitudFija)
            );
        }

        // Reserva un arreglo del tamaño requerido por el campo.
        byte[] bytesResultantes = new byte[longitudFija];
        // Inicializa todos los bytes con espacios de relleno.
        Arrays.fill(bytesResultantes, BYTE_ESPACIO);
        // Copia el texto al comienzo y deja el relleno al final.
        System.arraycopy(bytesOriginales, 0, bytesResultantes, 0, bytesOriginales.length);

        // Devuelve los bytes listos para escribirse en el fichero.
        return bytesResultantes;
    }

    /** Recupera secuencialmente los registros del fichero en la colección auxiliar.
     * @throws AppException Si no se puede leer el fichero de coches.
     */
    private void actualizarVariable() throws AppException{
        // Abre la base de datos en modo lectura y cierra el flujo al salir del bloque.
        try (RandomAccessFile arc = new RandomAccessFile(archivo, "r")){
           // Continúa mientras quede espacio para leer un registro completo.
           while (arc.getFilePointer() + TAMANO_REGISTRO <= arc.length()){
               // Reserva memoria para un registro binario completo.
               byte[] buffer = new byte[TAMANO_REGISTRO];
               // Lee exactamente los bytes del registro actual.
               arc.readFully(buffer);

               // Guarda el bloque leído en la colección auxiliar.
               listaDeRegistros.add(buffer);
           }
        } catch (IOException e) {
            // Convierte el error de entrada/salida al tipo de excepción de la aplicación.
            throw new AppException("No se pudo volcar la información de la base de datos");
        }
    }

    /**Escribe un texto en el fichero ocupando exactamente la longitud indicada en bytes.Adaptado del código del maestro para soportar tildes y caracteres especiales
     * @param raf Fichero binario en el que se escribirá.
     * @param texto Texto que se guardará.
     * @param longitudFija Número de bytes reservados para el campo.
     * @throws IOException Si ocurre un error al escribir en el fichero.
     */

    private static void escribirCampoFijo(RandomAccessFile raf, String texto, int longitudFija) throws IOException {
        //convierte el texto a bytes reales usando UTF-8
        byte[] bytesOriginales = texto.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        //prepara un array del tamaño exacto que necesita y lo rellena de espacios
        byte[] bytesResultantes = new byte[longitudFija];
        java.util.Arrays.fill(bytesResultantes, (byte) ' ');

        //copia los bytes del texto al array
        int longitudACopiar = Math.min(bytesOriginales.length, longitudFija);
        System.arraycopy(bytesOriginales, 0, bytesResultantes, 0, longitudACopiar);

        //escribe el bloque exacto en el fichero
        raf.write(bytesResultantes);
    }

    /** Busca secuencialmente una matrícula para impedir claves duplicadas.
     * @param raf Fichero binario que se recorrerá.
     * @param matriculaBuscada Matrícula que se quiere localizar.
     * @return {@code true} si la matrícula ya existe; {@code false} en caso contrario.
     * @throws IOException Si ocurre un error al leer el fichero.
     */
    private static boolean existeMatricula(RandomAccessFile raf, String matriculaBuscada) throws IOException {
        long totalRegistros = raf.length() / TAMANO_REGISTRO;

        for (long i = 0; i < totalRegistros; i++) {
            raf.seek(i * TAMANO_REGISTRO);
            String matriculaLeida = leerCampoFijo(raf, BYTES_MATRICULA);

            if (matriculaLeida.equalsIgnoreCase(matriculaBuscada)) {
                return true;
            }
        }
        return false;
    }

    /**Lee un número exacto de bytes del fichero y lo convierte a String.
     * @param raf Fichero del que se leerá el campo.
     * @param longitudFija Cantidad de bytes que se leerán.
     * @return El campo leído como texto, sin espacios de relleno.
     * @throws IOException Si no se pueden leer todos los bytes solicitados.
     */
    private static String leerCampoFijo(RandomAccessFile raf, int longitudFija) throws IOException {
        byte[] buffer = new byte[longitudFija];
        raf.readFully(buffer);

        // Convertimos los bytes leídos de vuelta a texto usando UTF-8 y quitamos los espacios de relleno
        return new String(buffer, java.nio.charset.StandardCharsets.UTF_8).trim();
    }

    /**
     * Comprueba si una matrícula ya existe en nuestro fichero binario (coches.dat).
     */
    /**
     * Copia un coche entero (71 bytes) de una posición a otra.
     */
    private static void copiarRegistro(RandomAccessFile raf, long posOrigen, long posDestino) throws IOException {
        // Caja para los 71 bytes
        byte[] buffer = new byte[TAMANO_REGISTRO];

        //lee el coche de origen
        raf.seek(posOrigen * TAMANO_REGISTRO);
        raf.readFully(buffer);

        //lo pego en el destino
        raf.seek(posDestino * TAMANO_REGISTRO);
        raf.write(buffer);
    }

    /** Inserta un coche en una posición y desplaza hacia atrás los registros posteriores.
     * @param matricula Matrícula del coche que se insertará.
     * @param marca Marca del coche.
     * @param modelo Modelo del coche.
     * @param posicion Índice en el que se insertará el coche, empezando por cero.
     * @throws IOException Si ocurre un error al modificar el fichero.
     */
    private static void insertar(String matricula, String marca, String modelo, int posicion) throws IOException {
        matricula = matricula.trim().toUpperCase();
        validarRegistro(matricula, marca, modelo);
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            int total = totalRegistros(raf);
            validarPosicion(posicion, total, true);
            if (existeMatricula(raf, matricula)) throw new IllegalArgumentException("La matrícula ya existe.");
            raf.setLength((long) (total + 1) * TAMANO_REGISTRO);
            // Se copia desde el final para no sobrescribir ningún registro aún pendiente de mover.
            for (int i = total; i > posicion; i--) copiarRegistro(raf, i - 1, i);
            raf.seek((long) posicion * TAMANO_REGISTRO);
            escribirRegistro(raf, matricula, marca, modelo);
        }
        System.out.println("Coche insertado.");
    }

    /** Ordena ascendentemente el fichero por matrícula.
     * @throws IOException Si ocurre un error al leer o escribir el fichero.
     */
    private static void ordenar() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            int total = totalRegistros(raf);
            byte[][] registros = new byte[total][TAMANO_REGISTRO];
            for (int i = 0; i < total; i++) { raf.seek((long) i * TAMANO_REGISTRO); raf.readFully(registros[i]); }
            Arrays.sort(registros, (a, b) -> matriculaDe(a).compareToIgnoreCase(matriculaDe(b)));
            raf.seek(0);
            for (byte[] registro : registros) raf.write(registro);
        }
        System.out.println("Fichero ordenado por matrícula.");
    }

    /** Extrae la matrícula del bloque binario de un registro.
     * @param registro Bytes del registro del que se extraerá la matrícula.
     * @return Matrícula almacenada en el registro.
     */
    private static String matriculaDe(byte[] registro) {
        return new String(registro, 0, BYTES_MATRICULA, StandardCharsets.UTF_8).trim();
    }

    /** Borra un registro buscando su matrícula.
     * @param matricula Matrícula del registro que se quiere borrar.
     * @throws IOException Si ocurre un error al acceder al fichero.
     */
    private static void borrarPorMatricula(String matricula) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            int posicion = buscarMatricula(raf, matricula);
            if (posicion < 0) throw new IllegalArgumentException("No se encontró esa matrícula.");
            borrarEnPosicion(raf, posicion);
        }
        System.out.println("Registro borrado.");
    }

    /** Borra un registro indicando su posición desde cero.
     * @param posicion Índice del registro que se quiere borrar, empezando por cero.
     * @throws IOException Si ocurre un error al acceder al fichero.
     */
    private static void borrarPorPosicion(int posicion) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            validarPosicion(posicion, totalRegistros(raf), false);
            borrarEnPosicion(raf, posicion);
        }
        System.out.println("Registro borrado.");
    }

    /** Elimina físicamente un registro, compactando los posteriores.
     * @param raf Fichero en el que se eliminará el registro.
     * @param posicion Índice del registro que se eliminará.
     * @throws IOException Si ocurre un error al mover o truncar los datos.
     */
    private static void borrarEnPosicion(RandomAccessFile raf, int posicion) throws IOException {
        int total = totalRegistros(raf);
        for (int i = posicion; i < total - 1; i++) copiarRegistro(raf, i + 1, i);
        raf.setLength((long) (total - 1) * TAMANO_REGISTRO);
    }

    /** Modifica marca y modelo de un registro y deja intacta su matrícula.
     * @param posicion Índice del coche que se modificará, empezando por cero.
     * @param marca Nueva marca del coche.
     * @param modelo Nuevo modelo del coche.
     * @throws IOException Si ocurre un error al acceder al fichero.
     */
    private static void modificar(int posicion, String marca, String modelo) throws IOException {
        validarCampo(marca, BYTES_MARCA, "Marca");
        validarCampo(modelo, BYTES_MODELO, "Modelo");
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            validarPosicion(posicion, totalRegistros(raf), false);
            raf.seek((long) posicion * TAMANO_REGISTRO);
            String matricula = leerCampoFijo(raf, BYTES_MATRICULA);
            raf.seek((long) posicion * TAMANO_REGISTRO);
            escribirRegistro(raf, matricula, marca, modelo);
        }
        System.out.println("Registro modificado; matrícula conservada.");
    }

    /** Muestra todos los registros con su índice.
     * @throws IOException Si ocurre un error al leer el fichero.
     */
    private static void listarCoches() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "r")) {
            int total = totalRegistros(raf);
            for (int i = 0; i < total; i++) {
                raf.seek((long) i * TAMANO_REGISTRO);
                System.out.printf("%d: %s | %s | %s%n", i, leerCampoFijo(raf, BYTES_MATRICULA), leerCampoFijo(raf, BYTES_MARCA), leerCampoFijo(raf, BYTES_MODELO));
            }
            System.out.println("Total: " + total);
        }
    }

    /** Escribe los campos de un registro en el orden fijado por el formato.
     * @param raf Fichero en el que se escribirá el registro.
     * @param matricula Matrícula que se escribirá.
     * @param marca Marca que se escribirá.
     * @param modelo Modelo que se escribirá.
     * @throws IOException Si ocurre un error al escribir el fichero.
     */
    private static void escribirRegistro(RandomAccessFile raf, String matricula, String marca, String modelo) throws IOException {
        escribirCampoFijo(raf, matricula, BYTES_MATRICULA);
        escribirCampoFijo(raf, marca, BYTES_MARCA);
        escribirCampoFijo(raf, modelo, BYTES_MODELO);
    }

    /** Devuelve el número de registros y detecta un último registro incompleto.
     * @param raf Fichero cuyo número de registros se calculará.
     * @return Cantidad de registros completos que contiene el fichero.
     * @throws IOException Si hay un registro incompleto o el fichero supera el límite admitido.
     */
    private static int totalRegistros(RandomAccessFile raf) throws IOException {
        if (raf.length() % TAMANO_REGISTRO != 0) throw new IOException("El fichero contiene un registro incompleto.");
        long cantidad = raf.length() / TAMANO_REGISTRO;
        if (cantidad > Integer.MAX_VALUE) throw new IOException("El fichero es demasiado grande.");
        return (int) cantidad;
    }

    /** Busca una matrícula de forma secuencial y devuelve su posición o -1.
     * @param raf Fichero que se recorrerá.
     * @param matricula Matrícula que se busca.
     * @return Índice del registro encontrado o {@code -1} si no existe.
     * @throws IOException Si ocurre un error al leer el fichero.
     */
    private static int buscarMatricula(RandomAccessFile raf, String matricula) throws IOException {
        int total = totalRegistros(raf);
        for (int i = 0; i < total; i++) {
            raf.seek((long) i * TAMANO_REGISTRO);
            if (leerCampoFijo(raf, BYTES_MATRICULA).equalsIgnoreCase(matricula)) return i;
        }
        return -1;
    }

    /** Comprueba que un campo no esté vacío y que quepa en bytes UTF-8.
     * @param texto Valor que se validará.
     * @param ancho Longitud máxima del campo en bytes UTF-8.
     * @param nombre Nombre del campo, usado en los mensajes de error.
     */
    private static void validarCampo(String texto, int ancho, String nombre) {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException(nombre + " no puede estar vacío.");
        if (texto.getBytes(StandardCharsets.UTF_8).length > ancho) throw new IllegalArgumentException(nombre + " supera " + ancho + " bytes.");
    }

    /** Comprueba que los tres campos del coche son correctos antes de escribirlo dentro del archivo .dat.
     * @param matricula Matrícula que se validará.
     * @param marca Marca que se validará.
     * @param modelo Modelo que se validará.
     */
    private static void validarRegistro(String matricula, String marca, String modelo) {
        validarCampo(matricula, BYTES_MATRICULA, "Matrícula");
        validarCampo(marca, BYTES_MARCA, "Marca");
        validarCampo(modelo, BYTES_MODELO, "Modelo");
    }

    /** Comprueba una posición de inserción o de un registro existente
     * @param posicion Índice que se comprobará.
     * @param total Número actual de registros.
     * @param permiteFinal Indica si se permite insertar justo después del último registro.
     */

    private static void validarPosicion(int posicion, int total, boolean permiteFinal) {
        int limite = permiteFinal ? total : total - 1;
        if (posicion < 0 || posicion > limite) throw new IllegalArgumentException("Posición fuera de rango.");
    }
}
