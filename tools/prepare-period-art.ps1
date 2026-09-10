param([string]$ArtDir = 'C:\Users\Lenovo\AppData\Local\Temp\ming-alarm-style\generated')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
public class QinglingPeriodArt {
  public static void Make(string source, string output) {
    using(var src = new Bitmap(source)) {
      int w = src.Width, h = src.Height;
      var data = src.LockBits(new Rectangle(0,0,w,h), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
      byte[] px = new byte[data.Stride * h];
      System.Runtime.InteropServices.Marshal.Copy(data.Scan0, px, 0, px.Length);
      src.UnlockBits(data);
      int stride = data.Stride;
      var colCount = new int[w];
      var rowCount = new int[h];
      for(int y=0;y<h;y++){
        int row = y*stride;
        for(int x=0;x<w;x++){
          int i = row + x*4;
          byte b=px[i], g=px[i+1], r=px[i+2];
          int minrb = Math.Min(r,b);
          double key = Math.Max(0, Math.Min(1, (minrb - g - 30)/140.0));
          int alpha = (int)(255*(1-key));
          if(alpha > 40){ colCount[x]++; rowCount[y]++; }
        }
      }
      int bestStart=0,bestEnd=0,bestLen=0,curStart=0; bool inRun=false;
      for(int x=0;x<w;x++){
        if(colCount[x]>0){ if(!inRun){inRun=true;curStart=x;} }
        else { if(inRun){inRun=false;int len=x-curStart;if(len>bestLen){bestLen=len;bestStart=curStart;bestEnd=x-1;}} }
      }
      if(inRun){int len=w-curStart;if(len>bestLen){bestLen=len;bestStart=curStart;bestEnd=w-1;}}
      int yMin=h,yMax=0;
      for(int y=0;y<h;y++) if(rowCount[y]>0){ yMin=y; break; }
      for(int y=h-1;y>=0;y--) if(rowCount[y]>0){ yMax=y; break; }
      int margin=12;
      int cx1=Math.Max(0,bestStart-margin), cx2=Math.Min(w-1,bestEnd+margin);
      int cy1=Math.Max(0,yMin-margin), cy2=Math.Min(h-1,yMax+margin);
      int cropW=cx2-cx1+1, cropH=cy2-cy1+1;
      using(var crop = new Bitmap(cropW,cropH,PixelFormat.Format32bppArgb)) {
        var cropData = crop.LockBits(new Rectangle(0,0,cropW,cropH), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
        byte[] outPx = new byte[cropData.Stride * cropH];
        for(int y=0;y<cropH;y++){
          int srcY=cy1+y, srcRow=srcY*stride, dstRow=y*cropData.Stride;
          for(int x=0;x<cropW;x++){
            int si=srcRow+(cx1+x)*4, di=dstRow+x*4;
            byte b=px[si], g=px[si+1], r=px[si+2];
            int minrb=Math.Min(r,b);
            double key=Math.Max(0,Math.Min(1,(minrb-g-30)/140.0));
            int alpha=(int)(255*(1-key));
            outPx[di]=b; outPx[di+1]=g; outPx[di+2]=r; outPx[di+3]=(byte)alpha;
          }
        }
        System.Runtime.InteropServices.Marshal.Copy(outPx,0,cropData.Scan0,outPx.Length);
        crop.UnlockBits(cropData);
        using(var canvas=new Bitmap(420,686,PixelFormat.Format32bppArgb))
        using(var g2=Graphics.FromImage(canvas)) {
          g2.InterpolationMode=InterpolationMode.HighQualityBicubic;
          float scale=Math.Min((float)(420-40)/cropW,(float)(686-40)/cropH);
          int dw=(int)(cropW*scale), dh=(int)(cropH*scale);
          g2.DrawImage(crop,new Rectangle((420-dw)/2,(686-dh)/2,dw,dh));
          canvas.Save(output,ImageFormat.Png);
        }
      }
    }
  }
}
'@
$dest = Join-Path (Split-Path $PSScriptRoot -Parent) 'app/src/main/res/drawable-nodpi'
New-Item -ItemType Directory -Force $dest | Out-Null
$map = @{
  'mascot_morning.png'   = 'waving.png'
  'mascot_noon.png'      = 'idle.png'
  'mascot_afternoon.png' = 'review-repair.png'
  'mascot_evening.png'   = 'waiting.png'
  'mascot_night.png'     = 'base.png'
}
foreach($entry in $map.GetEnumerator()){
  [QinglingPeriodArt]::Make((Join-Path $ArtDir $entry.Value),(Join-Path $dest $entry.Key))
  Write-Output "generated $($entry.Key) from $($entry.Value)"
}
