package groovy.util.concurrent

@Typed
class DivideAndConquerTest extends GroovyShellTestCase {
  // Test simple tree height calculation.
  class Node {
    List<Node> children

    def Node(children) {
      this.children = children
    }
  }

  Node createTree(int height, int depth) {
    def nChildren = ((int) (Math.random() * depth / height)) - 1
    def children = nChildren < 0 ? Collections.emptyList() : (0 .. nChildren).map { createTree(height+1, depth) }
    new Node(children)
  }

  void testTreeCreate() {
    assertNotNull createTree(1, 5)
  }

  SelfRecurringProblem calcHeightProblem(Node node) {
    [
       complex : { node.children.size() > 0 },
       sub     : { node.children.map { calcHeightProblem(it) } },
       solve   : { 1 },  // only called for leaves.
       combine : {results -> results.foldLeft(0){ int curr, int max -> Math.max(curr, max) } }
    ]
  }

  void testSolve() {
    def node = createTree(1, 5)
    def problem = calcHeightProblem(node)
    int height = new DivideAndConquerProblemSolver(problem, 3).solve()
    assertTrue(height > 0)
  }
}