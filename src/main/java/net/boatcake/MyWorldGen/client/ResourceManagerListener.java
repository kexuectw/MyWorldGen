package net.boatcake.MyWorldGen.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.boatcake.MyWorldGen.MyWorldGen;
import net.boatcake.MyWorldGen.Schematic;
import net.boatcake.MyWorldGen.WorldGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class ResourceManagerListener implements IResourceManagerReloadListener {
	private WorldGenerator worldGen;

	public ResourceManagerListener(WorldGenerator worldGen) {
		this.worldGen = worldGen;
	}

	public void register() {
		((SimpleReloadableResourceManager) Minecraft.getMinecraft()
				.getResourceManager()).registerReloadListener(this);
	}

	private static Object getFieldValueOfClass(Object o, Class c) {
		for (Field f : o.getClass().getDeclaredFields()) {
			boolean wasAccessible = f.isAccessible();
			f.setAccessible(true);
			Object value;
			try {
				value = f.get(o);
			} catch (IllegalArgumentException e) {
				continue;
			} catch (IllegalAccessException e) {
				continue;
			} finally {
				f.setAccessible(wasAccessible);
			}
			if (c.isInstance(value)) {
				return value;
			}
		}
		return null;
	}

	private static Method getMethodOfReturnClass(Object o, Class c) {
		for (Method m : o.getClass().getDeclaredMethods()) {
			boolean wasAccessible = m.isAccessible();
			m.setAccessible(true);
			if (m.getReturnType().equals(c)) {
				return m;
			}
			m.setAccessible(wasAccessible);
		}
		return null;
	}

	@Override
	public void onResourceManagerReload(IResourceManager manager) {
		// blehhhhhhhhhhh
		// TODO: Use AccessTransformers, maybe? Or just have a directory listing json like MC does for sounds
		try {
			if (!(manager instanceof SimpleReloadableResourceManager)) {
				return;
			}

			SimpleReloadableResourceManager simpleManager = (SimpleReloadableResourceManager) manager;
			Map domainResourceManagers = (Map) getFieldValueOfClass(
					simpleManager, Map.class);
			if (domainResourceManagers == null) {
				return;
			}

			FallbackResourceManager domainManager = (FallbackResourceManager) domainResourceManagers
					.get("myworldgen");
			if (domainManager == null) {
				return;
			}

			List resourcePacks = (List) getFieldValueOfClass(domainManager,
					List.class);
			for (Object o : resourcePacks) {
				if (!(o instanceof FileResourcePack)) {
					continue;
				}
				FileResourcePack pack = (FileResourcePack) o;
				Method getZip = getMethodOfReturnClass(pack, ZipFile.class);
				if (getZip == null) {
					continue;
				}
				ZipFile zf = (ZipFile) getZip.invoke(pack);
				try {
					ZipEntry worldGenDir = zf.getEntry(MyWorldGen.resourcePath
							+ "/");
					if (worldGenDir != null && worldGenDir.isDirectory()) {
						Set<Schematic> section = worldGen.getSection(new File(pack.getPackName()), 0);
						section.clear();
						for (Enumeration<? extends ZipEntry> e = zf.entries(); e
								.hasMoreElements();) {
							ZipEntry ze = e.nextElement();
							if (!ze.isDirectory()
									&& ze.getName().startsWith(
											worldGenDir.getName())) {
								worldGen.addSchemFromStream(section, zf.getInputStream(ze), ze.getName());
							}
						}
					}
				} catch (IOException e) {
					continue;
				}
			}
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
		// P.S.
		// BLEEHHHHHH
	}
}
