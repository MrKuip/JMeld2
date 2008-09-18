package org.jmeld.util.vc;

import java.io.*;

public interface VersionControlIF
{
  public BlameIF executeBlame(File file);

  public DiffIF executeDiff(File dir, boolean recursive);

  public StatusIF executeStatus(File dir, boolean recursive);

  public BaseFile getBaseFile(File dir);
}
