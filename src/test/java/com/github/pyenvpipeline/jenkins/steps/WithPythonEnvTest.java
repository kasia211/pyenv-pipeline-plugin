/*
 * The MIT License
 *
 * Copyright 2017 Colin Starner.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.github.pyenvpipeline.jenkins.steps;
import com.github.pyenvpipeline.jenkins.VirtualenvManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
public class WithPythonEnvTest {

    @Test
    public void testGetBaseDirectoryWithOneArgument() {
        VirtualenvManager virtualenvManager = VirtualenvManager.getInstance();
        Assert.assertEquals(".pyenv-Foo-Bar-python3", virtualenvManager.getRelativePythonDirectory("C:\\Foo\\Bar\\python3", "C:\\Foo\\Bar\\python3"));
        Assert.assertEquals(".pyenv-foo-bar-blah-python", virtualenvManager.getRelativePythonDirectory("/foo/bar/blah/python", "/foo/bar/blah/python"));
        Assert.assertEquals(".pyenv-foo-bar-blah-python", virtualenvManager.getRelativePythonDirectory("foo/bar/blah/python", "foo/bar/blah/python"));
        Assert.assertEquals(".pyenv-python3", virtualenvManager.getRelativePythonDirectory("python3", "python3"));
        Assert.assertEquals(".pyenv-Foo-Bar-python3", virtualenvManager.getRelativePythonDirectory("c:\\Foo\\Bar\\python3", "c:\\Foo\\Bar\\python3"));
        Assert.assertEquals(".pyenv-Foo-Bar-python3", virtualenvManager.getRelativePythonDirectory("D:\\Foo\\Bar\\python3", "D:\\Foo\\Bar\\python3"));
    }

    @Test
    public void testGetBaseDirectoryWithTwoArguments() {
        VirtualenvManager virtualenvManager = VirtualenvManager.getInstance();
        Assert.assertEquals(".pyenv-Foo-Bar-python3-Foo-Bar-python2", virtualenvManager.getRelativePythonDirectory("C:\\Foo\\Bar\\python3", "C:\\Foo\\Bar\\python2"));
        Assert.assertEquals(".pyenv-foo-bar-blah-python-foo-bar-blah-python2", virtualenvManager.getRelativePythonDirectory("/foo/bar/blah/python", "/foo/bar/blah/python2"));
        Assert.assertEquals(".pyenv-foo-bar-blah-python-foo-bar-blah-pypy", virtualenvManager.getRelativePythonDirectory("foo/bar/blah/python", "foo/bar/blah/pypy"));
        Assert.assertEquals(".pyenv-python3-python2", virtualenvManager.getRelativePythonDirectory("python3", "python2"));
        Assert.assertEquals(".pyenv-Foo-Bar-python3-Foo-Bar-python2", virtualenvManager.getRelativePythonDirectory("c:\\Foo\\Bar\\python3", "c:\\Foo\\Bar\\python2"));
        Assert.assertEquals(".pyenv-Foo-Bar-python3-Foo-Bar-python2", virtualenvManager.getRelativePythonDirectory("D:\\Foo\\Bar\\python3", "D:\\Foo\\Bar\\python2"));
    }
}
