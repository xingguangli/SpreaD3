package renderers.d3;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;

import settings.rendering.D3RendererSettings;

public class D3Renderer {

	private static final String D3_RENDERER_DIR = "renderers/d3/d3renderer/";
	private static final String D3_DATA_FILENAME = "data.json";
	private static final String HTML = "index.html";

	private D3RendererSettings settings;

	public D3Renderer(D3RendererSettings settings) {

		this.settings = settings;

	}// END: Constructor

	public void render() throws IOException {

		// Copy d3renderer dir to path/output
		String runningJarName = getRunningJarName();
		if (runningJarName != null) {

			JarFile jarfile = new JarFile(runningJarName);
			Enumeration<JarEntry> enumeration = jarfile.entries();

			while (enumeration.hasMoreElements()) {

				String destdir = settings.outputFilename;
				JarEntry je = enumeration.nextElement();

				if (je.toString().startsWith(D3_RENDERER_DIR)) {

					// System.out.println(je.getName());

					File fl = new File(destdir, je.getName());
					if (!fl.exists()) {
						fl.getParentFile().mkdirs();
						fl = new File(destdir, je.getName());
					}

					if (je.isDirectory()) {
						continue;
					}

					java.io.InputStream is = jarfile.getInputStream(je);
					java.io.FileOutputStream fo = new java.io.FileOutputStream(fl);

					while (is.available() > 0) {
						fo.write(is.read());
					}

					fo.close();
					is.close();
				}

			} // END: entries loop
			jarfile.close();

			// copy input.json to path/output/data/data.json
			File srcDir = new File(settings.jsonFilename);
			String destPath = settings.outputFilename.concat("/").concat(D3_RENDERER_DIR).concat(D3_DATA_FILENAME);
			File destDir = new File(destPath);
			FileUtils.copyFile(srcDir, destDir);

			// point system default browser to index.html
			String htmlPath = settings.outputFilename.concat("/").concat(D3_RENDERER_DIR).concat(HTML);
			openInBrowser(htmlPath);

		} else {// running from IDE

			String d3rendererPath = this.getClass().getResource("/".concat(D3_RENDERER_DIR)).getPath();
			File srcDir = new File(d3rendererPath);
			File destDir = new File(settings.outputFilename);
			FileUtils.copyDirectory(srcDir, destDir);

			// copy input.json to path/output/data/data.json
			srcDir = new File(settings.jsonFilename);
			destDir = new File(settings.outputFilename.concat("/").concat(D3_DATA_FILENAME));
			FileUtils.copyFile(srcDir, destDir);

			// point system default browser to index.html
			String htmlPath = settings.outputFilename.concat("/").concat(HTML);
			openInBrowser(htmlPath);

		}

	}// END: render

	private String getRunningJarName() {

		String className = this.getClass().getName().replace('.', '/');
		String classJar = this.getClass().getResource("/" + className + ".class").toString();

		if (classJar.startsWith("jar:")) {
			String vals[] = classJar.split("/");
			for (String val : vals) {
				if (val.contains("!")) {
					return val.substring(0, val.length() - 1);
				}
			}
		}

		return null;
	}// END: getRunningJarName

	public static void openInBrowser(String url) {

		try {

			File htmlFile = new File(url);
			URI uri = htmlFile.toURI();
			Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

			if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
				desktop.browse(uri);
			}

		} catch (MalformedURLException e) {

			handleBrowseException(url);

		} catch (IOException e) {

			handleBrowseException(url);

		} // END: try-catch

	}// END: openInBrowser

	private static void handleBrowseException(String url) {

		// Copy URL to the clipboard so the user can paste it into their browser
		StringSelection stringSelection = new StringSelection(url);
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		clpbrd.setContents(stringSelection, null);

		// Notify the user of the failure
		System.out.println("This program just tried to open a webpage: " + url);
		System.out.println("The URL has been copied to your clipboard, simply paste into your browser to access.");
	}// END: handleBrowseException

}// END: class
