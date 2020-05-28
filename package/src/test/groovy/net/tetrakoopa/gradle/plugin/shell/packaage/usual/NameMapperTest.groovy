package net.tetrakoopa.gradle.plugin.shell.packaage.usual

import junit.framework.Assert
import net.tetrakoopa.gradle.plugin.shell.packaage.usual.NameMapper
import org.junit.Test

class NameMapperTest {

	@Test
	void removeSuffix() {
		Assert.assertEquals('abcde', NameMapper.REMOVE_SUFFIX("abcde.efg"))
	}

	@Test
	void removeOnlyFirstLevelSuffix() {
		Assert.assertEquals('abcd.efg', NameMapper.REMOVE_SUFFIX("abcd.efg.hij"))
	}

}
