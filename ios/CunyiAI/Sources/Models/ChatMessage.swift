import Foundation

struct ChatMessage: Identifiable, Equatable {
    let id = UUID()
    let role: MessageRole
    let content: String
    let timestamp: Date = Date()
}

enum MessageRole: Equatable {
    case user
    case assistant
}
