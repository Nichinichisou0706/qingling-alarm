param([string]$Source = 'C:\Users\Lenovo\AppData\Local\Temp\ming-alarm-style\generated\base.png')
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
public class QinglingArt {
  public static void Make(string source,string output) {
    using(var src=new Bitmap(source)) using(var clean=new Bitmap(src.Width,src.Height,PixelFormat.Format32bppArgb)) {
      for(int y=0;y<src.Height;y++) for(int x=0;x<src.Width;x++) {
        Color p=src.GetPixel(x,y);
        double key=Math.Max(0,Math.Min(1,(Math.Min(p.R,p.B)-p.G-30)/140.0));
        int alpha=(int)(255*(1-key));
        clean.SetPixel(x,y,Color.FromArgb(alpha,p.R,p.G,p.B));
      }
      using(var body=new Bitmap(420,686)) using(var g=Graphics.FromImage(body)) {
        g.InterpolationMode=InterpolationMode.HighQualityBicubic;
        g.DrawImage(clean,new Rectangle(0,0,420,686));body.Save(output+"/mascot.png",ImageFormat.Png);
      }
      using(var icon=new Bitmap(432,432)) using(var g=Graphics.FromImage(icon)) {
        g.Clear(Color.FromArgb(229,242,236));g.InterpolationMode=InterpolationMode.HighQualityBicubic;
        g.DrawImage(clean,new Rectangle(48,42,336,355),new Rectangle(150,155,690,730),GraphicsUnit.Pixel);
        icon.Save(output+"/character_icon.png",ImageFormat.Png);
      }
    }
  }
}
'@
$dest=Join-Path (Split-Path $PSScriptRoot -Parent) 'app/src/main/res/drawable-nodpi'
New-Item -ItemType Directory -Force $dest | Out-Null
[QinglingArt]::Make($Source,$dest)
