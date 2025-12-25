package jadx.plugins.viewer.cfg;

import org.piccolo2d.PNode;
import org.piccolo2d.event.PBasicInputEventHandler;
import org.piccolo2d.event.PDragSequenceEventHandler;
import org.piccolo2d.event.PInputEvent;
import org.piccolo2d.event.PInputEventFilter;
import org.piccolo2d.event.PInputEventListener;
import org.piccolo2d.nodes.PImage;
import org.piccolo2d.util.PDimension;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.JComponent;

class MyListeners {

	/**
	 * 将 pImage 的大小适应 parent 的大小。若当前二者宽高不为0则立即计算，否则等 componentResized 时计算。
	 */
	public static void imageFit(JComponent parent, PImage pImage) {
		if (imageFitNow(parent, pImage))
			return;

		parent.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (imageFitNow(parent, pImage)) {
					parent.removeComponentListener(this);
				}
			}
		});
	}

	/**
	 * 同步处理图片适应边框大小，如果当前无法处理（parent 或 pImage 宽高为 0)，返回 false. 成功处理返回 true.
	 */
	private static boolean imageFitNow(JComponent parent, PImage pImage) {
		if (parent.getWidth() == 0 || parent.getHeight() == 0 || pImage.getWidth() == 0 || pImage.getHeight() == 0)
			return false;
		double widthScale = parent.getWidth() / pImage.getWidth();
		double heightScale = parent.getHeight() / pImage.getHeight();
		double fitScale = Math.min(widthScale, heightScale);

		// 复原至左上角，再以左上角为中心缩放
		pImage.setScale(1);
		pImage.setOffset(0, 0);
		pImage.scaleAboutPoint(fitScale, 0, 0);

		// 居中
		double centeredX = (parent.getWidth() - fitScale * pImage.getWidth()) / 2;
		double centeredY = (parent.getHeight() - fitScale * pImage.getHeight()) / 2;
		pImage.setOffset(centeredX, centeredY);
		return true;
	}


	public static PInputEventListener imageDrag(PNode imageNode) {
		return new ImageDrag(imageNode);
	}

	public static PInputEventListener imageZoom(PNode imageNode) {
		return new ImageZoom(imageNode);
	}


	private static class ImageDrag extends PDragSequenceEventHandler {
		private final PNode draggedNode;

		public ImageDrag(PNode imageNode) {
			draggedNode = imageNode;
			setEventFilter(new PInputEventFilter(InputEvent.BUTTON1_MASK)); // piccolo2d 依赖 jdk 版本很低，只能用废弃的 flag
		}

		protected void drag(final PInputEvent event) {
			super.drag(event);
			final PDimension d = event.getCanvasDelta();
			draggedNode.offset(d.getWidth(), d.getHeight());
		}
	}

	private static class ImageZoom extends PBasicInputEventHandler {
		private final PNode imageNode;

		public ImageZoom(PNode imageNode) {
			this.imageNode = imageNode;
			setEventFilter(new PInputEventFilter(InputEvent.BUTTON2_MASK));
		}

		@Override
		public void mouseClicked(PInputEvent event) {
			if (event.getButton() != MouseEvent.BUTTON2) return;
			event.getSourceSwingEvent().consume();
			// 鼠标中键复原
			imageNode.setScale(1);
			imageNode.setOffset(0, 0);
		}

		@Override
		public void mouseWheelRotated(PInputEvent event) {
			event.getSourceSwingEvent().consume();
			int rotation = event.getWheelRotation();
			Point2D mousePoint = event.getPosition();
			// 如果要用节点的 scaleAboutPoint, 要将全局坐标转为该节点对应的局部坐标。 camera 的话直接传全局坐标就行
			imageNode.globalToLocal(mousePoint);
			imageNode.scaleAboutPoint(rotation < 0 ? 1.2 : 0.8, mousePoint);
		}

		@Override
		public void mouseWheelRotatedByBlock(PInputEvent event) {
			CfgViewerPlugin.LOG.error(NLS.logNotSupportMouseWheelBlock);
		}
	}
}
