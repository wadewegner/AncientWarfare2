package net.shadowmage.ancientwarfare.core.gui.elements;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import net.shadowmage.ancientwarfare.core.util.RenderTools;
import net.shadowmage.ancientwarfare.modeler.gui.TextureManager;

public class TexturedRectangleLive extends GuiElement
{

TextureManager textureManager;
int tx, ty, u, v, uw, vh;
float u1, v1, u2, v2;

public TexturedRectangleLive(int topLeftX, int topLeftY, int width, int height, int tx, int ty, int u, int v, int uw, int vh)
  {
  super(topLeftX, topLeftY, width, height);
  this.tx = tx;//texture X size
  this.ty = ty;//texture Y size
  this.u = u;//start position in texture X axis
  this.v = v;//start position in texture Y axis
  this.uw = uw;//width of used texture 
  this.vh = vh;//height of used texture
  
  float perX = 1.f / ((float)tx);
  float perY = 1.f / ((float)ty);  
  u1 = ((float) u) * perX;
  v1 = ((float) v) * perY;
  u2 = (float)(u + uw) * perX;
  v2 = (float)(v + vh) * perY;
  textureManager = new TextureManager();
  textureManager.allocateTexture();
  }

@Override
public void render(int mouseX, int mouseY, float partialTick)
  {
  if(visible)
    {    
    textureManager.bindTexture();
    RenderTools.renderTexturedQuad(renderX, renderY, renderX+width, renderY+height, u1, v1, u2, v2);
    textureManager.resetBoundTexture();
    }
  }

public void saveTexture(File file)
  {
  textureManager.saveTexture(file);
  }

public void updateTexture(BufferedImage image)
  {
  textureManager.updateTextureContents(image);
  }

}