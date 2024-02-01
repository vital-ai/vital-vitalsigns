package ai.vital.vitalsigns.utils;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
/**
 * This source code is for copying a folder or files recursively using java nio
 */

import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class NIOUtils {

	public static void copyDirectoryToPath(final Path sourceDir, final Path target) throws IOException {
		
		EnumSet<FileVisitOption> options = EnumSet.of(FileVisitOption.FOLLOW_LINKS);

		// check first if source is a directory
		
		if(!Files.isDirectory(sourceDir)) throw new IOException("Expected source path to be a directory! " + sourceDir);
		
		if(!Files.isDirectory(target)) throw new IOException("Target directory does not exist or is not a directory!");
		
		final Path targetDirectory = Files.createDirectory(target.resolve(sourceDir.getFileName().toString()));
		
		Files.walkFileTree(sourceDir, options, Integer.MAX_VALUE, new FileVisitor<Path>() {
	 
			@Override
			public FileVisitResult postVisitDirectory(Path dir,
			IOException exc) throws IOException {

				return FileVisitResult.CONTINUE;
			}
			 
			@Override
			public FileVisitResult preVisitDirectory(Path dir,
			BasicFileAttributes attrs)  {

				CopyOption[] opt = new CopyOption[]{COPY_ATTRIBUTES,REPLACE_EXISTING};
				Path newDirectory = targetDirectory.resolve(sourceDir.relativize(dir).toString());
				try{
					Files.copy(dir, newDirectory,opt);
				}
				catch(FileAlreadyExistsException x){
				}
				catch(IOException x){
					return FileVisitResult.SKIP_SUBTREE;
				}
				 
				return CONTINUE;
			}
			 
			@Override
			public FileVisitResult visitFile(Path file,
			BasicFileAttributes attrs) throws IOException {
				
				CopyOption[] opt = new CopyOption[]{REPLACE_EXISTING,COPY_ATTRIBUTES};
				Path targetPath = targetDirectory.resolve(sourceDir.relativize(file).toString());
				Files.copy(file, targetPath, opt);
				return CONTINUE;
			}
			 
			@Override
			public FileVisitResult visitFileFailed(Path file,
			IOException exc) throws IOException {
				return CONTINUE;
			}
			});
	 
	}

	public static List<Path> listFilesRecursively(Path root) throws IOException {
		
		if( ! Files.isDirectory(root) ) throw new RuntimeException("Path is not a directory: " + root.toString());
		
		final List<Path> files = new ArrayList<Path>();
		
		Files.walkFileTree(root, new FileVisitor<Path>() {

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir,
					BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				files.add(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc)
					throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});

		return files;
		
	}

	
	public static void deleteDirectoryRecursively(Path directory) throws IOException {
		
		if( ! Files.isDirectory(directory) ) throw new RuntimeException("Path is not a directory: " + directory.toString());
		
		Files.walkFileTree(directory, new FileVisitor<Path>() {
		 
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
			throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
			 
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}
			 
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
			 
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});

	}

// SOURCE http://fahdshariff.blogspot.com/2011/08/java-7-working-with-zip-files.html
//  /**
//   * Returns a zip file system
//   * @param zipFilename to construct the file system from
//   * @param create true if the zip file should be created
//   * @return a zip file system
//   * @throws IOException
//   */
//   private static FileSystem createZipFileSystem(Path path, boolean create) throws IOException {
//      // convert the filename to a URI
//      final URI uri = URI.create("jar:file:" + path.toUri().getPath());
//    
//      final Map<String, String> env = new HashMap<String, String>();
//      if (create) {
//        env.put("create", "true");
//      }
//      return FileSystems.newFileSystem(uri, env);
//    }
//    
//    /**
//     * Unzips the specified zip file to the specified destination directory.
//     * Replaces any files in the destination, if they already exist.
//     * @param zipPath the name of the zip file to extract
//     * @param destFilename the directory to unzip to
//     * @throws IOException
//     */
//    public static void unzip(Path zipPath, final Path destDir)
//        throws IOException{
//     
////      final Path destDir = Paths.get(destDirname);
//      //if the destination doesn't exist, create it
//      if(Files.notExists(destDir)){
//        System.out.println(destDir + " does not exist. Creating...");
//        Files.createDirectories(destDir);
//      }
//     
//      try (FileSystem zipFileSystem = createZipFileSystem(zipPath, false)){
//        final Path root = zipFileSystem.getPath("/");
//     
//        //walk the zip file tree and copy files to the destination
//        Files.walkFileTree(root, new SimpleFileVisitor<Path>(){
//          @Override
//          public FileVisitResult visitFile(Path file,
//              BasicFileAttributes attrs) throws IOException {
//            String string = file.toString();
//            if(string.startsWith("/")) string = string.substring(1);
//            final Path destFile = destDir.resolve(string);
////            System.out.printf("Extracting file %s to %s\n", file, destFile);
//            Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
//            return FileVisitResult.CONTINUE;
//          }
//     
//          @Override
//          public FileVisitResult preVisitDirectory(Path dir,
//              BasicFileAttributes attrs) throws IOException {
//            String string = dir.toString();
//            if(string.startsWith("/")) string = string.substring(1);
//            final Path dirToCreate = destDir.resolve(string);
//            if(Files.notExists(dirToCreate)){
////              System.out.printf("Creating directory %s\n", dirToCreate);
//              Files.createDirectory(dirToCreate);
//            }
//            return FileVisitResult.CONTINUE;
//          }
//        });
//      }
//    }
}

