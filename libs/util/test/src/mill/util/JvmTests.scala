package mill.util

import mill.util.Jvm
import utest.{TestSuite, Tests, test}

import java.util.jar.{Attributes, JarFile}

object JvmTests extends TestSuite {

  val tests = Tests {

    test("createClasspathPassingJar") {
      val tmpDir = os.temp.dir()
      val aJar = tmpDir / "a.jar"
      assert(!os.exists(aJar))

      val dep1 = tmpDir / "dep-1.jar"
      val dep2 = tmpDir / "dep-2.jar"
      os.write(dep1, "JAR 1")
      os.write(dep2, "JAR 2")

      Jvm.createClasspathPassingJar(aJar, Seq(dep1, dep2))
      assert(os.exists(aJar))

      val jar = new JarFile(aJar.toIO)
      assert(jar.getManifest().getMainAttributes().containsKey(Attributes.Name.CLASS_PATH))
      assert(jar.getManifest().getMainAttributes().getValue(Attributes.Name.CLASS_PATH) ==
        Seq(dep1, dep2).map(_.toURL.toExternalForm()).mkString(" "))
    }

    test("launcherShellScript") {
      val script = Jvm.launcherShellScript(
        mainClass = "com.example.Main",
        shellClassPath = Seq("$0"),
        jvmArgs = Nil
      )
      // Must contain the Java invocation
      assert(script.contains("exec \"$JAVACMD\""))
      assert(script.contains("com.example.Main"))
      assert(script.contains("-cp \"$0\""))
      // Must NOT contain the polyglot batch header that triggers AV false positives
      assert(!script.contains("@ 2>/dev/null"))
      assert(!script.contains("goto BOF"))
      assert(!script.contains("2>nul"))
      // Must NOT contain Windows batch commands
      assert(!script.contains("@echo off"))
      assert(!script.contains(":BOF"))
      assert(!script.contains("exit /B"))
    }

    test("launcherShellScript-shebang") {
      val script = Jvm.launcherShellScript(
        mainClass = "com.example.Main",
        shellClassPath = Seq("$0"),
        jvmArgs = Nil,
        shebang = true
      )
      assert(script.startsWith("#!/usr/bin/env sh\n"))
    }

    test("launcherCmdScript") {
      val script = Jvm.launcherCmdScript(
        mainClass = "com.example.Main",
        cmdClassPath = Seq("%~dpnx0"),
        jvmArgs = Nil
      )
      // Must contain the Java invocation
      assert(script.contains("%JAVACMD%"))
      assert(script.contains("com.example.Main"))
      assert(script.contains("-cp \"%~dpnx0\""))
      // Must start with @echo off (pure batch, no polyglot header)
      assert(script.startsWith("@echo off"))
      // Must NOT contain the polyglot Unix shell header that triggers AV false positives
      assert(!script.contains("@ 2>/dev/null"))
      assert(!script.contains("goto BOF"))
      // Must NOT contain Unix shell commands
      assert(!script.contains("JAVACMD=\"java\""))
      assert(!script.contains("exec \"$JAVACMD\""))
    }

    test("launcherCmdScript-jvmArgs") {
      val script = Jvm.launcherCmdScript(
        mainClass = "com.example.Main",
        cmdClassPath = Seq("%~dpnx0"),
        jvmArgs = Seq("-Xmx512m")
      )
      assert(script.contains("-Xmx512m"))
    }

  }

}
